package com.company.finance.service.conversation;

import com.company.finance.agent.AgentConfig;
import com.company.finance.agent.tool.ExpenseQueryTool;
import com.company.finance.agent.tool.ExpenseSubmitTool;
import com.company.finance.agent.tool.InvoiceVerifyTool;
import com.company.finance.agent.tool.SalaryQueryTool;
import com.company.finance.agent.tool.SupplierQueryTool;
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
import kd.ai.nova.graph.agent.ReactAgent;
import kd.ai.nova.graph.agent.flow.agent.SupervisorAgent;
import kd.ai.nova.chat.ChatClient;
import kd.ai.nova.chat.ModelOptions;
import kd.ai.nova.chat.advisor.MessageChatMemoryAdvisor;
import kd.ai.nova.chat.advisor.SkillAdvisor;
import kd.ai.nova.core.tool.ToolCallbacks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
            + "请用专业、友好的中文回答问题。如果无法确定用户意图，请主动询问以明确需求。\n"
            + "如果遇到无法处理的问题，建议用户联系财务共享中心人工客服（电话：400-888-8888）。\n"
            + "不要编造不存在的电话号码或联系方式，只使用上述提供的信息。\n"
            + "【严禁编造数据】当用户询问具体的薪资金额、报销状态、发票信息等业务数据时，"
            + "你必须通过工具查询获取真实数据后再回答。"
            + "如果没有工具可用或工具调用失败，请告知用户：系统正在处理中，请稍后重试。"
            + "绝对禁止凭空编造任何金额、日期、姓名、部门等业务数据。";

    private final SessionMapper sessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AuditLogService auditLogService;
    private final DataMaskingService dataMaskingService;
    private final SatisfactionFeedbackService satisfactionFeedbackService;
    private final SupervisorAgent supervisorAgent;
    private final ReactAgent expenseAgent;
    private final ReactAgent invoiceAgent;
    private final ReactAgent salaryAgent;
    private final ReactAgent supplierAgent;
    private final ReactAgent guideAgent;
    private final ChatClient chatClient;
    /** 不带记忆 Advisor 的 ChatClient，用于闲聊回退（通过 messages() 手动传入历史） */
    private final ChatClient plainChatClient;
    private final ExpenseQueryTool expenseQueryTool;
    private final ExpenseSubmitTool expenseSubmitTool;
    private final InvoiceVerifyTool invoiceVerifyTool;
    private final SalaryQueryTool salaryQueryTool;
    private final SupplierQueryTool supplierQueryTool;
    private final ObjectMapper objectMapper;

    public ConversationService(SessionMapper sessionMapper,
                               ChatMessageMapper chatMessageMapper,
                               AuditLogService auditLogService,
                               DataMaskingService dataMaskingService,
                               SatisfactionFeedbackService satisfactionFeedbackService,
                               SupervisorAgent supervisorAgent,
                               @Qualifier("expenseAgent") ReactAgent expenseAgent,
                               @Qualifier("invoiceAgent") ReactAgent invoiceAgent,
                               @Qualifier("salaryAgent") ReactAgent salaryAgent,
                               @Qualifier("supplierAgent") ReactAgent supplierAgent,
                               @Qualifier("guideAgent") ReactAgent guideAgent,
                               ExpenseQueryTool expenseQueryTool,
                               ExpenseSubmitTool expenseSubmitTool,
                               InvoiceVerifyTool invoiceVerifyTool,
                               SalaryQueryTool salaryQueryTool,
                               SupplierQueryTool supplierQueryTool,
                               SkillAdvisor skillAdvisor,
                               ModelOptions modelOptions,
                               DataSource dataSource) {
        this.sessionMapper = sessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.auditLogService = auditLogService;
        this.dataMaskingService = dataMaskingService;
        this.satisfactionFeedbackService = satisfactionFeedbackService;
        this.supervisorAgent = supervisorAgent;
        this.expenseAgent = expenseAgent;
        this.invoiceAgent = invoiceAgent;
        this.salaryAgent = salaryAgent;
        this.supplierAgent = supplierAgent;
        this.guideAgent = guideAgent;
        this.expenseQueryTool = expenseQueryTool;
        this.expenseSubmitTool = expenseSubmitTool;
        this.invoiceVerifyTool = invoiceVerifyTool;
        this.salaryQueryTool = salaryQueryTool;
        this.supplierQueryTool = supplierQueryTool;
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
                .defaultAdvisors(memoryAdvisor, maskingAdvisor, handoffAdvisor, skillAdvisor)
                .defaultTools(ToolCallbacks.from(
                        expenseQueryTool, expenseSubmitTool,
                        invoiceVerifyTool, salaryQueryTool, supplierQueryTool))
                .defaultSystem(SYSTEM_PROMPT)
                .build();

        // 不带记忆的 ChatClient，用于闲聊回退（历史通过 messages() 手动传入）
        this.plainChatClient = ChatClient.builder(modelOptions)
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
     * @return 会话列表（置顶优先，再按创建时间降序）
     */
    public List<Session> getSessionsByEmployee(String employeeId) {
        return sessionMapper.selectByEmployeeId(employeeId);
    }

    /**
     * 重命名会话
     *
     * @param sessionId 会话 ID
     * @param title 新标题
     * @return 是否成功
     */
    public boolean renameSession(String sessionId, String title) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return false;
        }
        sessionMapper.updateTitle(sessionId, title);
        log.info("重命名会话: sessionId={}, title={}", sessionId, title);
        return true;
    }

    /**
     * 置顶/取消置顶会话
     *
     * @param sessionId 会话 ID
     * @param pinned 是否置顶
     * @return 是否成功
     */
    public boolean pinSession(String sessionId, boolean pinned) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return false;
        }
        LocalDateTime pinnedAt = pinned ? LocalDateTime.now() : null;
        sessionMapper.updatePinned(sessionId, pinned, pinnedAt);
        log.info("{}会话: sessionId={}", pinned ? "置顶" : "取消置顶", sessionId);
        return true;
    }

    /**
     * 删除会话及其关联的消息
     *
     * @param sessionId 会话 ID
     * @return 是否成功
     */
    public boolean deleteSession(String sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return false;
        }
        chatMessageMapper.deleteBySessionId(sessionId);
        sessionMapper.deleteById(sessionId);
        log.info("删除会话: sessionId={}", sessionId);
        return true;
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
            // 以用户首条消息作为会话标题，超长截取
            String title = message.length() > 50 ? message.substring(0, 50) + "..." : message;
            sessionMapper.updateTitle(activeSessionId, title);
        } else {
            activeSessionId = sessionId;
            // 如果会话还没有标题（旧会话兼容），用本次消息补上
            Session existing = sessionMapper.selectById(sessionId);
            if (existing != null && (existing.getTitle() == null || existing.getTitle().isEmpty())) {
                String title = message.length() > 50 ? message.substring(0, 50) + "..." : message;
                sessionMapper.updateTitle(sessionId, title);
            }
        }

        // 2. 持久化用户消息
        persistMessage(activeSessionId, MessageRole.USER, message, null);

        // 3. 判断意图：业务问题走带技能+工具的 chatClient，其他走 plainChatClient
        return Mono.fromCallable(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        // 判断是否为业务意图（结合当前消息和会话历史）
                        if (isBusinessIntent(message) || resolveAgentFromContext(message, activeSessionId) != null) {
                            String instruction = routeToInstruction(message);
                            log.info("业务意图走 chatClient（技能+工具）: message='{}'", message);
                            try {
                                String contextualMessage = buildContextualMessage(activeSessionId, message);
                                kd.ai.nova.core.model.chat.response.ChatResponse chatResponse = chatClient.request()
                                        .system(instruction)
                                        .user(contextualMessage)
                                        .call();
                                if (chatResponse != null && chatResponse.aiMessage() != null) {
                                    String text = chatResponse.aiMessage().text();
                                    if (text != null && !text.isEmpty()) {
                                        return text;
                                    }
                                }
                                log.warn("chatClient 业务调用返回空响应");
                            } catch (Exception e) {
                                log.error("chatClient 业务调用异常", e);
                            }
                            return "抱歉，系统暂时无法查询到相关数据，请稍后重试或联系人工客服（电话：400-888-8888）。";
                        }

                        // 非业务意图，用 plainChatClient 直接对话
                        log.info("非业务意图: message='{}', sessionId={}, 走 plainChatClient", message, activeSessionId);
                        java.util.List<kd.ai.nova.core.data.message.ChatMessage> historyMessages = buildHistoryMessages(activeSessionId);
                        log.info("历史消息数量: sessionId={}, count={}", activeSessionId, historyMessages.size());
                        kd.ai.nova.core.model.chat.response.ChatResponse chatResponse = plainChatClient.request()
                                .system(SYSTEM_PROMPT)
                                .messages(historyMessages)
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
     * 从对象中提取文本内容（支持 AiMessage.text() 和普通 String）
     */
    private String extractText(Object obj) {
        if (obj == null) return null;
        // 先尝试反射调用 text() 方法
        try {
            java.lang.reflect.Method textMethod = obj.getClass().getMethod("text");
            Object text = textMethod.invoke(obj);
            if (text != null && !text.toString().isEmpty()) {
                return text.toString();
            }
        } catch (Exception e) {
            // 没有 text() 方法，继续处理
        }
        // 回退到 toString，清理 AiMessage 包装格式
        String str = obj.toString();
        if (str.contains("AiMessage")) {
            // 清理 "AiMessage { text = \"...\" , toolExecutionRequests = null }" 格式
            str = str.replaceAll("^\\s*AiMessage\\s*\\{\\s*text\\s*=\\s*\"", "");
            str = str.replaceAll("\"\\s*,?\\s*toolExecutionRequests\\s*=\\s*null\\s*\\}\\s*$", "");
            str = str.replaceAll("\"\\s*\\}\\s*$", "");
        }
        return str;
    }

    /**
     * 判断用户消息是否包含业务意图关键词
     */
    private boolean isBusinessIntent(String message) {
        if (message == null || message.trim().length() < 2) {
            return false;
        }
        String[] businessKeywords = {
            "报销", "借款", "付款", "发票", "验真", "开票",
            "工资", "薪资", "个税", "社保", "公积金",
            "供应商", "审批", "退回", "材料", "表单",
            "单据", "差旅", "补贴", "预算", "费用",
            "查询", "提交", "申请", "核对", "验证"
        };
        for (String keyword : businessKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据用户消息关键词路由到对应的子 Agent。
     * 直接调用子 Agent 可以确保用户原始消息完整传递，避免 SupervisorAgent 改写消息。
     *
     * @return 匹配的子 Agent，无法确定时返回 null（走 SupervisorAgent 兜底）
     */
    private ReactAgent routeToAgent(String message) {
        return classifyIntentByLLM(message);
    }

    /**
     * 使用 LLM 进行意图分类，返回对应的 ReactAgent。
     * <p>
     * 让模型只返回一个 agent 名称（或 NONE），避免硬编码关键词遗漏和误匹配。
     * </p>
     */
    private static final String INTENT_CLASSIFY_PROMPT =
            "你是一个意图分类器。根据用户消息，判断应该由哪个智能体处理，只返回智能体名称，不要返回其他内容。\n\n"
            + "可选智能体：\n"
            + "expense-agent：报销、借款、付款、差旅、出差、住宿标准、餐饮补贴、交通费、费用报销制度\n"
            + "invoice-agent：发票验真、发票查询、发票类型、开票、增值税发票\n"
            + "salary-agent：工资、薪资、个税、社保、公积金、税率、专项扣除\n"
            + "supplier-agent：供应商查询、供应商核对\n"
            + "guide-agent：单据退回、材料补齐、表单填写、审批流程、审批步骤\n"
            + "NONE：闲聊、打招呼、无法判断\n\n"
            + "只返回上述名称之一，不要解释。";

    private ReactAgent classifyIntentByLLM(String message) {
        try {
            kd.ai.nova.core.model.chat.response.ChatResponse response = plainChatClient.request()
                    .system(INTENT_CLASSIFY_PROMPT)
                    .user(message)
                    .call();

            if (response == null || response.aiMessage() == null) {
                log.warn("意图分类 LLM 返回空响应");
                return null;
            }

            String agentName = response.aiMessage().text().trim().toLowerCase();
            log.info("LLM 意图分类结果: message='{}', agent='{}'", message, agentName);

            switch (agentName) {
                case "expense-agent":
                    return expenseAgent;
                case "invoice-agent":
                    return invoiceAgent;
                case "salary-agent":
                    return salaryAgent;
                case "supplier-agent":
                    return supplierAgent;
                case "guide-agent":
                    return guideAgent;
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("LLM 意图分类异常，回退到关键词匹配", e);
            return fallbackRouteByKeyword(message);
        }
    }

    /**
     * 关键词匹配兜底，仅在 LLM 分类失败时使用。
     */
    private ReactAgent fallbackRouteByKeyword(String message) {
        if (message.contains("报销") || message.contains("借款") || message.contains("付款")
                || message.contains("差旅") || message.contains("费用") || message.contains("补贴")
                || message.contains("出差") || message.contains("住宿")) {
            return expenseAgent;
        }
        if (message.contains("发票") || message.contains("验真") || message.contains("开票")) {
            return invoiceAgent;
        }
        if (message.contains("工资") || message.contains("薪资") || message.contains("个税")
                || message.contains("社保") || message.contains("公积金")) {
            return salaryAgent;
        }
        if (message.contains("供应商")) {
            return supplierAgent;
        }
        if (message.contains("退回") || message.contains("材料") || message.contains("表单")
                || message.contains("单据")) {
            return guideAgent;
        }
        return null;
    }


    /**
     * 结合当前消息和会话历史上下文，解析应路由到的 ReactAgent。
     * <p>
     * 先尝试从当前消息匹配业务关键词；若未匹配，则回溯会话历史消息，
     * 判断用户是否正在进行多轮业务对话（如先说"薪资查询"，再提供参数"100000 2026-03"）。
     * </p>
     */
    private ReactAgent resolveAgentFromContext(String message, String sessionId) {
        // 1. 先尝试从当前消息直接匹配
        if (isBusinessIntent(message)) {
            ReactAgent agent = routeToAgent(message);
            if (agent != null) {
                return agent;
            }
        }

        // 2. 当前消息未匹配，回溯会话历史寻找业务上下文
        try {
            List<ChatMessage> history = chatMessageMapper.selectBySessionId(sessionId);
            if (history != null && !history.isEmpty()) {
                // 从最近的消息往前找，最多回溯 6 条（3 轮对话）
                int startIdx = Math.max(0, history.size() - 6);
                for (int i = history.size() - 1; i >= startIdx; i--) {
                    ChatMessage msg = history.get(i);
                    if (msg.getContent() != null) {
                        ReactAgent agent = routeToAgent(msg.getContent());
                        if (agent != null) {
                            log.info("从会话历史匹配到业务上下文: sessionId={}, historyMsg='{}', agent='{}'",
                                    sessionId, msg.getContent().substring(0, Math.min(30, msg.getContent().length())),
                                    agent.name());
                            return agent;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("回溯会话历史失败: sessionId={}", sessionId, e);
        }

        return null;
    }


    /**
     * 根据用户消息关键词返回对应的业务 system prompt。
     * 配合 toolChatClient 使用，让 LLM 在正确的角色下调用工具。
     */
    private String routeToInstruction(String message) {
        if (message.contains("报销") || message.contains("借款") || message.contains("付款")
                || message.contains("差旅") || message.contains("费用") || message.contains("补贴")) {
            return AgentConfig.EXPENSE_INSTRUCTION;
        }
        if (message.contains("发票") || message.contains("验真") || message.contains("开票")) {
            return AgentConfig.INVOICE_INSTRUCTION;
        }
        if (message.contains("工资") || message.contains("薪资") || message.contains("个税")
                || message.contains("社保") || message.contains("公积金")) {
            return AgentConfig.SALARY_INSTRUCTION;
        }
        if (message.contains("供应商")) {
            return AgentConfig.SUPPLIER_INSTRUCTION;
        }
        if (message.contains("退回") || message.contains("材料") || message.contains("表单")
                || message.contains("单据")) {
            return AgentConfig.GUIDE_INSTRUCTION;
        }
        return SYSTEM_PROMPT;
    }

    /**
     * 从数据库加载会话历史消息，转换为 AI-Nova ChatMessage 列表
     */
    /**
     * 构建带上下文的消息，将最近几轮对话历史摘要拼接到当前消息前。
     * 仅保留最近 MAX_CONTEXT_ROUNDS 轮（一问一答为一轮），避免上下文过长。
     * 如果当前消息本身包含完整参数（如报销单号+员工ID），则不需要历史上下文。
     */
    private static final int MAX_CONTEXT_ROUNDS = 3;

    private String buildContextualMessage(String sessionId, String currentMessage) {
        try {
            List<ChatMessage> dbMessages = chatMessageMapper.selectBySessionId(sessionId);
            if (dbMessages == null || dbMessages.size() <= 1) {
                // 只有当前这条消息（刚 persist 的），无历史
                return currentMessage;
            }

            // 排除最后一条（就是当前刚持久化的用户消息），取之前的历史
            List<ChatMessage> history = dbMessages.subList(0, dbMessages.size() - 1);

            // 只取最近 MAX_CONTEXT_ROUNDS 轮
            int maxMessages = MAX_CONTEXT_ROUNDS * 2;
            int startIdx = Math.max(0, history.size() - maxMessages);
            List<ChatMessage> recentHistory = history.subList(startIdx, history.size());

            if (recentHistory.isEmpty()) {
                return currentMessage;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("【以下是本次会话的最近对话记录，供你理解上下文】\n");
            for (ChatMessage msg : recentHistory) {
                String role = msg.getRole() == MessageRole.USER ? "用户" : "助手";
                // 截断过长的历史消息，避免上下文爆炸
                String content = msg.getContent();
                if (content != null && content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
                sb.append(role).append(": ").append(content).append("\n");
            }
            sb.append("【对话记录结束】\n\n");
            sb.append("当前用户请求: ").append(currentMessage);

            return sb.toString();
        } catch (Exception e) {
            log.warn("构建上下文消息失败，回退到原始消息: sessionId={}", sessionId, e);
            return currentMessage;
        }
    }

    private java.util.List<kd.ai.nova.core.data.message.ChatMessage> buildHistoryMessages(String sessionId) {
        java.util.List<kd.ai.nova.core.data.message.ChatMessage> result = new ArrayList<>();
        try {
            List<ChatMessage> dbMessages = chatMessageMapper.selectBySessionId(sessionId);
            if (dbMessages != null) {
                for (ChatMessage msg : dbMessages) {
                    if (msg.getRole() == MessageRole.USER) {
                        result.add(new kd.ai.nova.core.data.message.UserMessage(msg.getContent()));
                    } else if (msg.getRole() == MessageRole.ASSISTANT) {
                        result.add(kd.ai.nova.core.data.message.AiMessage.from(msg.getContent()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("加载会话历史消息失败: sessionId={}", sessionId, e);
        }
        return result;
    }

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
