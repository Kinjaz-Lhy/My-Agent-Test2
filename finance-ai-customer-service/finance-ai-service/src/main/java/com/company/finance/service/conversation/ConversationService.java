package com.company.finance.service.conversation;

import com.company.finance.common.dto.ChatStreamResponse;
import com.company.finance.common.enums.MessageRole;
import com.company.finance.common.enums.SessionStatus;
import com.company.finance.domain.entity.ChatMessage;
import com.company.finance.domain.entity.Session;
import com.company.finance.infrastructure.mapper.ChatMessageMapper;
import com.company.finance.infrastructure.mapper.SessionMapper;
import com.company.finance.infrastructure.memory.ChatMemoryConfig;
import com.company.finance.service.advisor.DataMaskingAdvisor;
import com.company.finance.service.advisor.HumanHandoffAdvisor;
import com.company.finance.service.audit.AuditLogService;
import com.company.finance.service.advisor.DataMaskingService;
import com.company.finance.service.satisfaction.SatisfactionFeedbackService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kd.ai.nova.graph.OverAllState;
import kd.ai.nova.graph.agent.flow.agent.SupervisorAgent;
import kd.ai.nova.chat.ChatClient;
import kd.ai.nova.chat.ModelOptions;
import kd.ai.nova.chat.advisor.MessageChatMemoryAdvisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * 会话管理服务
 * <p>
 * 负责会话的创建、恢复、关闭，以及流式对话处理。
 * 集成 ChatClient + Advisor 链（记忆 → 脱敏 → 人工转接），
 * 并通过 SupervisorAgent 进行智能体调度。
 * </p>
 *
 * @see <a href="需求 1.1, 1.2, 1.3, 1.4, 1.6, 7.2">自然对话交互 &amp; 系统集成</a>
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    /** 连续未满足需求触发人工转接的轮次阈值 */
    private static final int HANDOFF_THRESHOLD = 3;

    /** 流式输出每个 SSE 事件的字符分块大小 */
    private static final int STREAM_CHUNK_SIZE = 50;

    /** 系统提示词 */
    private static final String SYSTEM_PROMPT =
            "你是企业财务共享中心的智能客服助手，负责帮助员工处理财务咨询和业务办理。\n"
            + "请用专业、友好的中文回答问题。如果无法确定用户意图，请主动询问以明确需求。";

    private final SessionMapper sessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AuditLogService auditLogService;
    private final DataMaskingService dataMaskingService;
    private final SatisfactionFeedbackService satisfactionFeedbackService;
    private final SupervisorAgent supervisorAgent;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ConversationService(SessionMapper sessionMapper,
                               ChatMessageMapper chatMessageMapper,
                               AuditLogService auditLogService,
                               DataMaskingService dataMaskingService,
                               SatisfactionFeedbackService satisfactionFeedbackService,
                               SupervisorAgent supervisorAgent,
                               ModelOptions modelOptions,
                               DataSource dataSource) {
        this.sessionMapper = sessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.auditLogService = auditLogService;
        this.dataMaskingService = dataMaskingService;
        this.satisfactionFeedbackService = satisfactionFeedbackService;
        this.supervisorAgent = supervisorAgent;
        this.objectMapper = new ObjectMapper();

        // 构建 Advisor 链：记忆 → 脱敏 → 人工转接
        MessageChatMemoryAdvisor memoryAdvisor = ChatMemoryConfig.createMemoryAdvisor(dataSource);
        DataMaskingAdvisor maskingAdvisor = new DataMaskingAdvisor(dataMaskingService);
        HumanHandoffAdvisor handoffAdvisor = new HumanHandoffAdvisor(
                HANDOFF_THRESHOLD,
                new HumanHandoffAdvisor.HandoffCallback() {
                    @Override
                    public void onHandoff(String sessionId, String context) {
                        log.info("触发人工转接: sessionId={}", sessionId);
                    }
                }
        );

        this.chatClient = ChatClient.builder(modelOptions)
                .defaultAdvisors(memoryAdvisor, maskingAdvisor, handoffAdvisor)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    // ==================== 会话生命周期管理 ====================

    /**
     * 创建新会话
     *
     * @param employeeId   员工 ID
     * @param departmentId 部门 ID（可选）
     * @return 新创建的会话
     */
    public Session createSession(String employeeId, String departmentId) {
        LocalDateTime now = LocalDateTime.now();
        Session session = Session.builder()
                .sessionId(UUID.randomUUID().toString())
                .employeeId(employeeId)
                .departmentId(departmentId)
                .status(SessionStatus.ACTIVE)
                .context(new HashMap<String, Object>())
                .createdAt(now)
                .updatedAt(now)
                .build();

        sessionMapper.insert(session, serializeContext(session.getContext()));
        log.info("创建新会话: sessionId={}, employeeId={}", session.getSessionId(), employeeId);
        return session;
    }

    /**
     * 恢复已有会话
     * <p>
     * 加载会话基本信息和历史消息列表，仅 ACTIVE 和 TRANSFERRED 状态的会话可恢复。
     * </p>
     *
     * @param sessionId 会话 ID
     * @return 恢复的会话（含消息历史），不存在或已关闭时返回 null
     */
    public Session resumeSession(String sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            log.warn("会话不存在: sessionId={}", sessionId);
            return null;
        }
        if (session.getStatus() == SessionStatus.CLOSED) {
            log.warn("会话已关闭，无法恢复: sessionId={}", sessionId);
            return null;
        }
        List<ChatMessage> messages = chatMessageMapper.selectBySessionId(sessionId);
        session.setMessages(messages);
        log.debug("恢复会话: sessionId={}, 消息数={}", sessionId, messages.size());
        return session;
    }

    /**
     * 关闭会话
     * <p>
     * 关闭会话后，如果该会话尚未提交满意度评价，则标记需要触发满意度评价请求。
     * </p>
     *
     * @param sessionId 会话 ID
     * @return 关闭结果：包含是否成功关闭和是否需要满意度评价
     */
    public CloseSessionResult closeSession(String sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null || session.getStatus() == SessionStatus.CLOSED) {
            return new CloseSessionResult(false, false);
        }
        sessionMapper.updateStatus(sessionId, SessionStatus.CLOSED.name());
        log.info("关闭会话: sessionId={}", sessionId);

        // 检查是否已存在满意度反馈，若不存在则触发评价请求
        boolean needsSatisfactionEvaluation = !satisfactionFeedbackService.feedbackExists(sessionId);
        if (needsSatisfactionEvaluation) {
            log.info("触发满意度评价请求: sessionId={}", sessionId);
        }
        return new CloseSessionResult(true, needsSatisfactionEvaluation);
    }

    /**
     * 会话关闭结果
     */
    public static class CloseSessionResult {
        private final boolean closed;
        private final boolean satisfactionEvaluationRequired;

        public CloseSessionResult(boolean closed, boolean satisfactionEvaluationRequired) {
            this.closed = closed;
            this.satisfactionEvaluationRequired = satisfactionEvaluationRequired;
        }

        /** 会话是否成功关闭 */
        public boolean isClosed() {
            return closed;
        }

        /** 是否需要触发满意度评价 */
        public boolean isSatisfactionEvaluationRequired() {
            return satisfactionEvaluationRequired;
        }
    }

    /**
     * 获取员工的会话列表
     *
     * @param employeeId 员工 ID
     * @return 会话列表（按创建时间降序）
     */
    public List<Session> getSessionsByEmployee(String employeeId) {
        return sessionMapper.selectByEmployeeId(employeeId);
    }

    /**
     * 获取会话详情（含消息历史）
     *
     * @param sessionId 会话 ID
     * @return 会话详情，不存在时返回 null
     */
    public Session getSessionDetail(String sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session != null) {
            List<ChatMessage> messages = chatMessageMapper.selectBySessionId(sessionId);
            session.setMessages(messages);
        }
        return session;
    }

    /**
     * 获取配置了 Advisor 链的 ChatClient 实例。
     * <p>
     * 可用于不需要智能体调度的简单对话场景，
     * 已集成记忆、脱敏和人工转接 Advisor。
     * </p>
     *
     * @return ChatClient 实例
     */
    public ChatClient getChatClient() {
        return chatClient;
    }

    // ==================== 流式对话处理 ====================

    /**
     * 流式对话处理
     * <p>
     * 通过 SupervisorAgent 进行智能体调度，将 AI 响应以 SSE 流式事件返回。
     * 处理流程：
     * 1. 若无 sessionId 则自动创建新会话
     * 2. 持久化用户消息
     * 3. 调用 SupervisorAgent.invoke(String) 获取 AI 响应
     * 4. 对响应执行脱敏处理
     * 5. 持久化 AI 响应消息并记录审计日志
     * 6. 将脱敏后的响应按字符分块以 SSE 事件流返回
     * </p>
     *
     * @param employeeId 员工 ID
     * @param sessionId  会话 ID（为空时自动创建新会话）
     * @param message    用户消息内容
     * @return SSE 事件流
     */
    public Flux<ServerSentEvent<ChatStreamResponse>> streamChat(
            String employeeId, String sessionId, String message) {

        // 1. 确保会话存在
        final String activeSessionId;
        if (sessionId == null || sessionId.isEmpty()) {
            Session newSession = createSession(employeeId, null);
            activeSessionId = newSession.getSessionId();
        } else {
            activeSessionId = sessionId;
        }

        // 2. 持久化用户消息
        persistMessage(activeSessionId, MessageRole.USER, message, null);

        // 3. 先用 ChatClient 判断意图，闲聊直接回复，业务问题走 SupervisorAgent
        return Mono.fromCallable(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        // 先尝试 SupervisorAgent 调度
                        try {
                            Optional<OverAllState> result = supervisorAgent.invoke(message);
                            if (result.isPresent()) {
                                Map<String, Object> data = result.get().data();
                                if (data != null) {
                                    // 从 agent_output 提取
                                    Object agentOutput = data.get("agent_output");
                                    if (agentOutput != null && !agentOutput.toString().isEmpty()) {
                                        return agentOutput.toString();
                                    }
                                    // 从 messages 中找 AiMessage
                                    Object msgs = data.get("messages");
                                    if (msgs instanceof java.util.List) {
                                        java.util.List<?> msgList = (java.util.List<?>) msgs;
                                        for (int i = msgList.size() - 1; i >= 0; i--) {
                                            Object msg = msgList.get(i);
                                            if (msg != null && msg.getClass().getSimpleName().contains("AiMessage")) {
                                                Object text = msg.getClass().getMethod("text").invoke(msg);
                                                if (text != null && !text.toString().isEmpty()) {
                                                    return text.toString();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("SupervisorAgent 调用异常，回退到 ChatClient", e);
                        }

                        // SupervisorAgent 没有返回有效回复，用 ChatClient 直接对话
                        log.info("SupervisorAgent 未返回有效回复，使用 ChatClient 直接对话");
                        kd.ai.nova.core.model.chat.response.ChatResponse chatResponse = chatClient.request()
                                .system(SYSTEM_PROMPT)
                                .user(message)
                                .call();
                        if (chatResponse != null && chatResponse.aiMessage() != null) {
                            String text = chatResponse.aiMessage().text();
                            if (text != null && !text.isEmpty()) {
                                return text;
                            }
                        }
                        return "抱歉，系统暂时无法处理您的请求。";
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(new java.util.function.Function<String, Flux<ServerSentEvent<ChatStreamResponse>>>() {
                    @Override
                    public Flux<ServerSentEvent<ChatStreamResponse>> apply(String responseContent) {
                        // 4. 脱敏处理
                        String maskedContent = dataMaskingService.mask(responseContent);

                        // 5. 持久化 AI 响应并记录审计日志
                        persistMessage(activeSessionId, MessageRole.ASSISTANT, responseContent, null);
                        auditLogService.logConversation(
                                activeSessionId, employeeId, "CHAT",
                                message, responseContent, maskedContent);

                        // 6. 流式输出脱敏后的内容
                        return streamResponse(activeSessionId, maskedContent);
                    }
                })
                .onErrorResume(new java.util.function.Function<Throwable, Flux<ServerSentEvent<ChatStreamResponse>>>() {
                    @Override
                    public Flux<ServerSentEvent<ChatStreamResponse>> apply(Throwable e) {
                        log.error("对话处理异常: sessionId={}", activeSessionId, e);
                        String errorMsg = "系统暂时无法处理您的请求，请稍后重试或联系人工客服。";
                        return Flux.just(buildSseEvent(activeSessionId,
                                ChatStreamResponse.builder()
                                        .sessionId(activeSessionId)
                                        .content(errorMsg)
                                        .done(true)
                                        .build()));
                    }
                });
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 持久化对话消息到数据库
     *
     * @param sessionId 会话 ID
     * @param role      消息角色
     * @param content   消息内容
     * @param intent    识别的意图（可选）
     */
    private void persistMessage(String sessionId, MessageRole role, String content, String intent) {
        try {
            ChatMessage chatMessage = ChatMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .role(role)
                    .content(content)
                    .intent(intent)
                    .metadata(new HashMap<String, Object>())
                    .timestamp(LocalDateTime.now())
                    .build();
            chatMessageMapper.insert(chatMessage, serializeMetadata(chatMessage.getMetadata()));
        } catch (Exception e) {
            log.error("持久化消息失败: sessionId={}, role={}", sessionId, role, e);
        }
    }

    /**
     * 从 SupervisorAgent 调用结果中提取响应文本
     *
     * @param result Agent invoke 返回的 Optional&lt;OverAllState&gt;
     * @return 响应文本
     */
    private String extractResponse(Optional<OverAllState> result) {
        if (!result.isPresent()) {
            return "抱歉，系统暂时无法处理您的请求。";
        }
        OverAllState state = result.get();
        Map<String, Object> data = state.data();
        log.info("=== OverAllState data keys: {}", data != null ? data.keySet() : "null");
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof List) {
                    List<?> list = (List<?>) val;
                    log.info("  key='{}', type=List, size={}", entry.getKey(), list.size());
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        if (item != null) {
                            log.info("    [{}] class={}, toString={}", i, item.getClass().getName(),
                                    item.toString().length() > 200 ? item.toString().substring(0, 200) + "..." : item.toString());
                        }
                    }
                } else {
                    log.info("  key='{}', type={}, value={}", entry.getKey(),
                            val != null ? val.getClass().getName() : "null",
                            val != null ? (val.toString().length() > 200 ? val.toString().substring(0, 200) + "..." : val.toString()) : "null");
                }
            }
        }
        if (data == null || data.isEmpty()) {
            return "抱歉，系统暂时无法处理您的请求。";
        }
        // SupervisorAgent 返回的 OverAllState 中，尝试从 messages 提取最后一条 AI 响应
        Object messages = data.get("messages");
        if (messages instanceof List) {
            List<?> msgList = (List<?>) messages;
            // 从后往前找最后一条 AiMessage（跳过 UserMessage、ToolMessage 等）
            for (int i = msgList.size() - 1; i >= 0; i--) {
                Object msg = msgList.get(i);
                if (msg == null) continue;
                try {
                    // 检查消息类型：AiMessage 的类名包含 "AiMessage"
                    String className = msg.getClass().getSimpleName();
                    if (className.contains("AiMessage")) {
                        java.lang.reflect.Method textMethod = msg.getClass().getMethod("text");
                        Object text = textMethod.invoke(msg);
                        if (text != null && !text.toString().isEmpty()) {
                            return text.toString();
                        }
                    }
                } catch (Exception e) {
                    // ignore, try next
                }
            }
            // 回退：如果没找到 AiMessage，取最后一条非空消息
            for (int i = msgList.size() - 1; i >= 0; i--) {
                Object msg = msgList.get(i);
                if (msg == null) continue;
                try {
                    java.lang.reflect.Method textMethod = msg.getClass().getMethod("text");
                    Object text = textMethod.invoke(msg);
                    if (text != null && !text.toString().isEmpty()) {
                        return text.toString();
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        // 兼容其他可能的键名
        Object output = data.get("output");
        if (output != null) {
            return output.toString();
        }
        Object response = data.get("response");
        if (response != null) {
            return response.toString();
        }
        Object content = data.get("content");
        if (content != null) {
            return content.toString();
        }
        return "抱歉，系统暂时无法处理您的请求。";
    }

    /**
     * 将响应内容按字符分块，生成 SSE 事件流（模拟打字机效果）
     *
     * @param sessionId 会话 ID
     * @param content   完整响应内容
     * @return SSE 事件流
     */
    private Flux<ServerSentEvent<ChatStreamResponse>> streamResponse(String sessionId, String content) {
        if (content == null || content.isEmpty()) {
            return Flux.just(buildSseEvent(sessionId,
                    ChatStreamResponse.builder().sessionId(sessionId).content("").done(true).build()));
        }

        // 按 STREAM_CHUNK_SIZE 分块
        final List<String> chunks = new ArrayList<String>();
        for (int i = 0; i < content.length(); i += STREAM_CHUNK_SIZE) {
            int end = Math.min(i + STREAM_CHUNK_SIZE, content.length());
            chunks.add(content.substring(i, end));
        }

        return Flux.fromIterable(chunks)
                .index()
                .map(new java.util.function.Function<reactor.util.function.Tuple2<Long, String>,
                        ServerSentEvent<ChatStreamResponse>>() {
                    @Override
                    public ServerSentEvent<ChatStreamResponse> apply(
                            reactor.util.function.Tuple2<Long, String> tuple) {
                        long index = tuple.getT1();
                        String chunk = tuple.getT2();
                        boolean isLast = (index == chunks.size() - 1);
                        return buildSseEvent(sessionId,
                                ChatStreamResponse.builder()
                                        .sessionId(sessionId)
                                        .content(chunk)
                                        .done(isLast)
                                        .build());
                    }
                });
    }

    /**
     * 构建 SSE 事件
     *
     * @param sessionId 会话 ID
     * @param response  响应数据
     * @return ServerSentEvent 包装
     */
    private ServerSentEvent<ChatStreamResponse> buildSseEvent(String sessionId, ChatStreamResponse response) {
        return ServerSentEvent.<ChatStreamResponse>builder()
                .id(sessionId)
                .data(response)
                .build();
    }

    /**
     * 序列化上下文 Map 为 JSON 字符串
     *
     * @param context 上下文 Map
     * @return JSON 字符串，序列化失败时返回 "{}"
     */
    private String serializeContext(Map<String, Object> context) {
        if (context == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            log.warn("序列化上下文失败", e);
            return "{}";
        }
    }

    /**
     * 序列化元数据 Map 为 JSON 字符串
     *
     * @param metadata 元数据 Map
     * @return JSON 字符串，序列化失败时返回 "{}"
     */
    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("序列化元数据失败", e);
            return "{}";
        }
    }
}
