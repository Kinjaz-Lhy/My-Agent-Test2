package com.company.finance.service.conversation;

import com.company.finance.common.dto.ChatStreamResponse;
import com.company.finance.common.enums.SessionStatus;
import com.company.finance.domain.entity.Session;
import com.company.finance.infrastructure.mapper.SessionMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * HumanHandoffService 单元测试
 */
class HumanHandoffServiceTest {

    private SessionMapper sessionMapper;
    private HumanHandoffService service;

    @BeforeEach
    void setUp() {
        sessionMapper = mock(SessionMapper.class);
        service = new HumanHandoffService(sessionMapper);
    }

    @Test
    void isSessionTransferredReturnsTrueForTransferredSession() {
        Session session = Session.builder()
                .sessionId("sess-001")
                .status(SessionStatus.TRANSFERRED)
                .build();
        when(sessionMapper.selectById("sess-001")).thenReturn(session);

        assertThat(service.isSessionTransferred("sess-001")).isTrue();
    }

    @Test
    void isSessionTransferredReturnsFalseForActiveSession() {
        Session session = Session.builder()
                .sessionId("sess-001")
                .status(SessionStatus.ACTIVE)
                .build();
        when(sessionMapper.selectById("sess-001")).thenReturn(session);

        assertThat(service.isSessionTransferred("sess-001")).isFalse();
    }

    @Test
    void isSessionTransferredReturnsFalseForClosedSession() {
        Session session = Session.builder()
                .sessionId("sess-001")
                .status(SessionStatus.CLOSED)
                .build();
        when(sessionMapper.selectById("sess-001")).thenReturn(session);

        assertThat(service.isSessionTransferred("sess-001")).isFalse();
    }

    @Test
    void isSessionTransferredReturnsFalseForNullSession() {
        when(sessionMapper.selectById("sess-missing")).thenReturn(null);

        assertThat(service.isSessionTransferred("sess-missing")).isFalse();
    }

    @Test
    void isSessionTransferredReturnsFalseOnException() {
        when(sessionMapper.selectById("sess-err")).thenThrow(new RuntimeException("DB error"));

        assertThat(service.isSessionTransferred("sess-err")).isFalse();
    }

    @Test
    void streamWaitingStatusCompletesWhenSessionNotTransferred() {
        // Session is ACTIVE, so the stream should complete after first interval tick
        // because takeWhile will evaluate to false
        Session session = Session.builder()
                .sessionId("sess-001")
                .status(SessionStatus.ACTIVE)
                .build();
        when(sessionMapper.selectById("sess-001")).thenReturn(session);

        StepVerifier.withVirtualTime(() -> service.streamWaitingStatus("sess-001"))
                .thenAwait(Duration.ofSeconds(30))
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void streamWaitingStatusEmitsEventForTransferredSession() {
        // First call: TRANSFERRED (takeWhile passes), second call: CLOSED (takeWhile stops)
        Session transferred = Session.builder()
                .sessionId("sess-001")
                .status(SessionStatus.TRANSFERRED)
                .build();
        Session closed = Session.builder()
                .sessionId("sess-001")
                .status(SessionStatus.CLOSED)
                .build();
        when(sessionMapper.selectById("sess-001"))
                .thenReturn(transferred)
                .thenReturn(closed);

        StepVerifier.withVirtualTime(() -> service.streamWaitingStatus("sess-001"))
                .thenAwait(Duration.ofSeconds(30))
                .assertNext(event -> {
                    assertThat(event.event()).isEqualTo(HumanHandoffService.EVENT_TYPE_WAITING);
                    ChatStreamResponse data = event.data();
                    assertThat(data).isNotNull();
                    assertThat(data.getSessionId()).isEqualTo("sess-001");
                    assertThat(data.getContent()).contains("正在为您转接人工客服");
                    assertThat(data.getDone()).isFalse();
                })
                .thenAwait(Duration.ofSeconds(30))
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }
}
