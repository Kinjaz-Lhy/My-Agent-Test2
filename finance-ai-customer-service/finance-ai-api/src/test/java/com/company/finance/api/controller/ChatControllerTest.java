package com.company.finance.api.controller;

import com.company.finance.api.security.UserPrincipal;
import com.company.finance.common.dto.ChatRequest;
import com.company.finance.common.dto.ChatStreamResponse;
import com.company.finance.common.dto.FeedbackRequest;
import com.company.finance.common.enums.SessionStatus;
import com.company.finance.domain.entity.SatisfactionFeedback;
import com.company.finance.domain.entity.Session;
import com.company.finance.service.conversation.ConversationService;
import com.company.finance.service.conversation.ConversationService.CloseSessionResult;
import com.company.finance.service.conversation.HumanHandoffService;
import com.company.finance.service.satisfaction.SatisfactionFeedbackService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ChatController 单元测试
 */
class ChatControllerTest {

    private ConversationService conversationService;
    private SatisfactionFeedbackService satisfactionFeedbackService;
    private HumanHandoffService humanHandoffService;
    private ChatController chatController;
    private UserPrincipal testUser;

    @BeforeEach
    void setUp() {
        conversationService = mock(ConversationService.class);
        satisfactionFeedbackService = mock(SatisfactionFeedbackService.class);
        humanHandoffService = mock(HumanHandoffService.class);
        chatController = new ChatController(conversationService, satisfactionFeedbackService, humanHandoffService);
        testUser = new UserPrincipal("EMP001", "DEPT-FIN", Collections.singletonList("ROLE_EMPLOYEE"));
    }

    // ==================== streamChat ====================

    @Test
    void streamChatShouldReturnSseFlux() {
        ChatRequest request = ChatRequest.builder()
                .sessionId("sess-001")
                .message("我的报销单状态？")
                .build();

        ServerSentEvent<ChatStreamResponse> event = ServerSentEvent.<ChatStreamResponse>builder()
                .id("sess-001")
                .data(ChatStreamResponse.builder()
                        .sessionId("sess-001")
                        .content("您的报销单已审批通过")
                        .done(true)
                        .build())
                .build();

        when(conversationService.streamChat("EMP001", "sess-001", "我的报销单状态？"))
                .thenReturn(Flux.just(event));

        Flux<ServerSentEvent<ChatStreamResponse>> result = chatController.streamChat(request, testUser);

        StepVerifier.create(result)
                .assertNext(sse -> {
                    assertThat(sse.data()).isNotNull();
                    assertThat(sse.data().getSessionId()).isEqualTo("sess-001");
                    assertThat(sse.data().getContent()).isEqualTo("您的报销单已审批通过");
                    assertThat(sse.data().getDone()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void streamChatWithoutSessionIdShouldCreateNewSession() {
        ChatRequest request = ChatRequest.builder()
                .message("你好")
                .build();

        ServerSentEvent<ChatStreamResponse> event = ServerSentEvent.<ChatStreamResponse>builder()
                .id("new-sess")
                .data(ChatStreamResponse.builder()
                        .sessionId("new-sess")
                        .content("你好！")
                        .done(true)
                        .build())
                .build();

        when(conversationService.streamChat("EMP001", null, "你好"))
                .thenReturn(Flux.just(event));

        Flux<ServerSentEvent<ChatStreamResponse>> result = chatController.streamChat(request, testUser);

        StepVerifier.create(result)
                .assertNext(sse -> assertThat(sse.data().getContent()).isEqualTo("你好！"))
                .verifyComplete();
    }

    // ==================== getSessions ====================

    @Test
    void getSessionsShouldReturnEmployeeSessions() {
        Session s1 = Session.builder().sessionId("sess-001").employeeId("EMP001")
                .status(SessionStatus.ACTIVE).createdAt(LocalDateTime.now()).build();
        Session s2 = Session.builder().sessionId("sess-002").employeeId("EMP001")
                .status(SessionStatus.CLOSED).createdAt(LocalDateTime.now()).build();

        when(conversationService.getSessionsByEmployee("EMP001"))
                .thenReturn(Arrays.asList(s1, s2));

        Mono<ResponseEntity<List<Session>>> result = chatController.getSessions(testUser);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).hasSize(2);
                    assertThat(response.getBody().get(0).getSessionId()).isEqualTo("sess-001");
                })
                .verifyComplete();
    }

    @Test
    void getSessionsShouldReturnEmptyListWhenNoSessions() {
        when(conversationService.getSessionsByEmployee("EMP001"))
                .thenReturn(Collections.emptyList());

        Mono<ResponseEntity<List<Session>>> result = chatController.getSessions(testUser);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).isEmpty();
                })
                .verifyComplete();
    }

    // ==================== getSessionDetail ====================

    @Test
    void getSessionDetailShouldReturnSessionWithMessages() {
        Session session = Session.builder().sessionId("sess-001").employeeId("EMP001")
                .status(SessionStatus.ACTIVE).createdAt(LocalDateTime.now()).build();

        when(conversationService.getSessionDetail("sess-001")).thenReturn(session);

        Mono<ResponseEntity<Session>> result = chatController.getSessionDetail("sess-001");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody().getSessionId()).isEqualTo("sess-001");
                })
                .verifyComplete();
    }

    @Test
    void getSessionDetailShouldReturn404WhenNotFound() {
        when(conversationService.getSessionDetail("nonexistent")).thenReturn(null);

        Mono<ResponseEntity<Session>> result = chatController.getSessionDetail("nonexistent");

        StepVerifier.create(result)
                .assertNext(response -> assertThat(response.getStatusCodeValue()).isEqualTo(404))
                .verifyComplete();
    }

    // ==================== closeSession ====================

    @Test
    void closeSessionShouldReturnResultWithSatisfactionFlag() {
        when(conversationService.closeSession("sess-001"))
                .thenReturn(new CloseSessionResult(true, true));

        Mono<ResponseEntity<CloseSessionResult>> result = chatController.closeSession("sess-001");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody().isClosed()).isTrue();
                    assertThat(response.getBody().isSatisfactionEvaluationRequired()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void closeSessionShouldReturn404WhenSessionNotFound() {
        when(conversationService.closeSession("nonexistent"))
                .thenReturn(new CloseSessionResult(false, false));

        Mono<ResponseEntity<CloseSessionResult>> result = chatController.closeSession("nonexistent");

        StepVerifier.create(result)
                .assertNext(response -> assertThat(response.getStatusCodeValue()).isEqualTo(404))
                .verifyComplete();
    }

    // ==================== submitFeedback ====================

    @Test
    void submitFeedbackShouldReturnCreatedFeedback() {
        FeedbackRequest request = FeedbackRequest.builder().score(5).comment("很满意").build();

        SatisfactionFeedback feedback = SatisfactionFeedback.builder()
                .feedbackId("fb-001")
                .sessionId("sess-001")
                .employeeId("EMP001")
                .score(5)
                .comment("很满意")
                .createdAt(LocalDateTime.now())
                .build();

        when(satisfactionFeedbackService.submitFeedback("sess-001", "EMP001", 5, "很满意"))
                .thenReturn(feedback);

        Mono<ResponseEntity<SatisfactionFeedback>> result =
                chatController.submitFeedback("sess-001", request, testUser);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody().getScore()).isEqualTo(5);
                    assertThat(response.getBody().getComment()).isEqualTo("很满意");
                    assertThat(response.getBody().getSessionId()).isEqualTo("sess-001");
                })
                .verifyComplete();
    }

    @Test
    void submitFeedbackWithoutCommentShouldWork() {
        FeedbackRequest request = FeedbackRequest.builder().score(3).build();

        SatisfactionFeedback feedback = SatisfactionFeedback.builder()
                .feedbackId("fb-002")
                .sessionId("sess-001")
                .employeeId("EMP001")
                .score(3)
                .createdAt(LocalDateTime.now())
                .build();

        when(satisfactionFeedbackService.submitFeedback("sess-001", "EMP001", 3, null))
                .thenReturn(feedback);

        Mono<ResponseEntity<SatisfactionFeedback>> result =
                chatController.submitFeedback("sess-001", request, testUser);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody().getScore()).isEqualTo(3);
                    assertThat(response.getBody().getComment()).isNull();
                })
                .verifyComplete();
    }
}
