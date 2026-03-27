package com.company.finance.service.advisor;

import kd.ai.nova.chat.advisor.api.CallAdvisor;
import kd.ai.nova.chat.advisor.api.CallAdvisorChain;
import kd.ai.nova.chat.ChatClientRequest;
import kd.ai.nova.chat.ChatClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 人工转接 Advisor
 * <p>
 * 监控连续未满足用户需求的轮次，当达到阈值（默认 3 轮）时
 * 自动触发人工坐席转接流程，并将完整对话上下文传递给人工坐席。
 * </p>
 * <p>
 * 判定"未满足需求"的策略：检测 AI 响应中是否包含不确定/无法处理的关键词，
 * 如"抱歉"、"无法"、"不确定"、"建议联系"等。
 * </p>
 *
 * @see <a href="需求 4.5">人工转接触发机制</a>
 */
public class HumanHandoffAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(HumanHandoffAdvisor.class);

    private static final String ADVISOR_NAME = "HumanHandoffAdvisor";
    private static final int DEFAULT_ORDER = 90;

    /** 连续未满足需求的轮次阈值 */
    static final int DEFAULT_THRESHOLD = 3;

    /** 未满足需求的关键词列表 */
    static final List<String> UNSATISFIED_KEYWORDS = Collections.unmodifiableList(Arrays.asList(
            "抱歉", "无法处理", "无法解决", "不确定", "建议联系人工",
            "暂时无法", "超出处理能力", "无法回答", "暂未找到"
    ));

    private final int threshold;

    /** 按会话 ID 记录连续未满足轮次 */
    private final Map<String, Integer> unsatisfiedCountMap = new ConcurrentHashMap<>();

    /** 人工转接回调 */
    private final HandoffCallback handoffCallback;

    /**
     * 人工转接回调接口
     */
    @FunctionalInterface
    public interface HandoffCallback {
        /**
         * 触发人工转接
         *
         * @param sessionId      会话 ID
         * @param conversationContext 对话上下文摘要
         */
        void onHandoff(String sessionId, String conversationContext);
    }

    public HumanHandoffAdvisor() {
        this(DEFAULT_THRESHOLD, null);
    }

    public HumanHandoffAdvisor(int threshold, HandoffCallback handoffCallback) {
        this.threshold = threshold;
        this.handoffCallback = handoffCallback;
    }

    @Override
    public String getName() {
        return ADVISOR_NAME;
    }

    @Override
    public int getOrder() {
        return DEFAULT_ORDER;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);

        try {
            String sessionId = extractSessionId(request);
            String responseContent = response.chatResponse().aiMessage().text();

            if (responseContent != null && isUnsatisfiedResponse(responseContent)) {
                int count = unsatisfiedCountMap.merge(sessionId, 1, Integer::sum);
                log.debug("会话 {} 连续未满足轮次: {}/{}", sessionId, count, threshold);

                if (count >= threshold) {
                    log.info("会话 {} 连续 {} 轮未满足需求，触发人工转接", sessionId, count);
                    triggerHandoff(sessionId, request, responseContent);
                    unsatisfiedCountMap.remove(sessionId);
                }
            } else {
                // 用户需求被满足，重置计数
                unsatisfiedCountMap.remove(sessionId);
            }
        } catch (Exception e) {
            log.warn("人工转接检测异常，不影响正常响应", e);
        }

        return response;
    }

    /**
     * 检测响应是否表示未满足用户需求
     */
    boolean isUnsatisfiedResponse(String responseContent) {
        if (responseContent == null || responseContent.isEmpty()) {
            return false;
        }
        return UNSATISFIED_KEYWORDS.stream()
                .anyMatch(responseContent::contains);
    }

    /**
     * 获取指定会话的连续未满足轮次
     */
    public int getUnsatisfiedCount(String sessionId) {
        return unsatisfiedCountMap.getOrDefault(sessionId, 0);
    }

    /**
     * 重置指定会话的未满足计数
     */
    public void resetCount(String sessionId) {
        unsatisfiedCountMap.remove(sessionId);
    }

    private String extractSessionId(ChatClientRequest request) {
        // 从请求上下文中提取会话 ID，默认使用 "default"
        Map<String, Object> context = request.context();
        if (context != null && context.containsKey("sessionId")) {
            return String.valueOf(context.get("sessionId"));
        }
        return "default";
    }

    private void triggerHandoff(String sessionId, ChatClientRequest request, String lastResponse) {
        String context = buildConversationContext(request, lastResponse);
        if (handoffCallback != null) {
            handoffCallback.onHandoff(sessionId, context);
        }
    }

    private String buildConversationContext(ChatClientRequest request, String lastResponse) {
        StringBuilder sb = new StringBuilder();
        sb.append("对话上下文摘要：\n");
        sb.append("- 最后用户消息: ").append(extractUserMessage(request)).append("\n");
        sb.append("- 最后系统响应: ").append(lastResponse).append("\n");
        sb.append("- 转接原因: 连续 ").append(threshold).append(" 轮未能满足用户需求");
        return sb.toString();
    }

    private String extractUserMessage(ChatClientRequest request) {
        try {
            // AI-Nova 1.1: extract last user message from ChatRequest.messages()
            return request.chatRequest().messages().stream()
                    .filter(m -> m.type() == kd.ai.nova.core.data.message.ChatMessageType.USER)
                    .reduce((a, b) -> b)
                    .map(m -> m.text())
                    .orElse("（无法提取）");
        } catch (Exception e) {
            return "（无法提取）";
        }
    }
}
