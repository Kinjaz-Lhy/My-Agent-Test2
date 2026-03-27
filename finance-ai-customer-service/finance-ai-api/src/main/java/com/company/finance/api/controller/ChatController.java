package com.company.finance.api.controller;

import com.company.finance.api.security.UserPrincipal;
import com.company.finance.common.dto.ChatRequest;
import com.company.finance.common.dto.ChatStreamResponse;
import com.company.finance.common.dto.FeedbackRequest;
import com.company.finance.domain.entity.SatisfactionFeedback;
import com.company.finance.domain.entity.Session;
import com.company.finance.service.conversation.ConversationService;
import com.company.finance.service.conversation.ConversationService.CloseSessionResult;
import com.company.finance.service.conversation.HumanHandoffService;
import com.company.finance.service.satisfaction.SatisfactionFeedbackService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

/**
 * 对话控制器
 * <p>
 * 提供 SSE 流式对话、会话管理和满意度评价端点。
 * 所有端点需要认证（Spring Security 配置 /api/v1/chat/** → authenticated）。
 * </p>
 *
 * @see <a href="需求 1.1, 6.3, 7.2, 9.1">自然对话交互、满意度评价、WebFlux、前端交互</a>
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ConversationService conversationService;
    private final SatisfactionFeedbackService satisfactionFeedbackService;
    private final HumanHandoffService humanHandoffService;

    public ChatController(ConversationService conversationService,
                          SatisfactionFeedbackService satisfactionFeedbackService,
                          HumanHandoffService humanHandoffService) {
        this.conversationService = conversationService;
        this.satisfactionFeedbackService = satisfactionFeedbackService;
        this.humanHandoffService = humanHandoffService;
    }

    /** Dev 模式下 user 可能为 null，返回默认 UserPrincipal */
    private UserPrincipal resolveUser(UserPrincipal user) {
        if (user != null) return user;
        return new UserPrincipal("dev-user", "DEPT-DEV", Collections.singletonList("ROLE_OPERATOR"));
    }

    /**
     * SSE 流式对话端点
     * <p>
     * 接收用户消息，通过 ConversationService 调度智能体处理，
     * 以 SSE 事件流返回分块响应（打字机效果）。
     * </p>
     *
     * @param request 对话请求（含可选 sessionId 和消息内容）
     * @param user    当前认证用户
     * @return SSE 事件流
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamResponse>> streamChat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        UserPrincipal u = resolveUser(user);
        return conversationService.streamChat(
                u.getEmployeeId(),
                request.getSessionId(),
                request.getMessage()
        );
    }

    /**
     * 获取当前用户的会话列表
     *
     * @param user 当前认证用户
     * @return 会话列表（按创建时间降序）
     */
    @GetMapping("/sessions")
    public Mono<ResponseEntity<List<Session>>> getSessions(
            @AuthenticationPrincipal UserPrincipal user) {
        UserPrincipal u = resolveUser(user);
        List<Session> sessions = conversationService.getSessionsByEmployee(u.getEmployeeId());
        return Mono.just(ResponseEntity.ok(sessions));
    }

    /**
     * 获取会话详情（含消息历史）
     *
     * @param sessionId 会话 ID
     * @return 会话详情，不存在时返回 404
     */
    @GetMapping("/sessions/{sessionId}")
    public Mono<ResponseEntity<Session>> getSessionDetail(
            @PathVariable String sessionId) {
        Session session = conversationService.getSessionDetail(sessionId);
        if (session == null) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        return Mono.just(ResponseEntity.ok(session));
    }

    /**
     * 关闭会话
     * <p>
     * 关闭后返回结果中包含是否需要触发满意度评价。
     * </p>
     *
     * @param sessionId 会话 ID
     * @return 关闭结果
     */
    @PostMapping("/sessions/{sessionId}/close")
    public Mono<ResponseEntity<CloseSessionResult>> closeSession(
            @PathVariable String sessionId) {
        CloseSessionResult result = conversationService.closeSession(sessionId);
        if (!result.isClosed()) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        return Mono.just(ResponseEntity.ok(result));
    }

    /**
     * 等待人工坐席接入的 SSE 状态推送
     * <p>
     * 当会话处于 TRANSFERRED 状态时，每 30 秒推送等待状态消息。
     * 会话状态变更后自动停止推送。
     * </p>
     *
     * @param sessionId 会话 ID
     * @return SSE 事件流（等待状态消息）
     */
    @GetMapping(value = "/sessions/{sessionId}/waiting", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamResponse>> streamWaitingStatus(
            @PathVariable String sessionId) {
        return humanHandoffService.streamWaitingStatus(sessionId);
    }

    /**
     * 提交满意度评价
     *
     * @param sessionId 会话 ID
     * @param request   评价请求（评分 1-5 + 可选文字反馈）
     * @param user      当前认证用户
     * @return 创建的满意度反馈实体
     */
    @PostMapping("/sessions/{sessionId}/feedback")
    public Mono<ResponseEntity<SatisfactionFeedback>> submitFeedback(
            @PathVariable String sessionId,
            @Valid @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        UserPrincipal u = resolveUser(user);
        SatisfactionFeedback feedback = satisfactionFeedbackService.submitFeedback(
                sessionId,
                u.getEmployeeId(),
                request.getScore(),
                request.getComment()
        );
        return Mono.just(ResponseEntity.ok(feedback));
    }
}
