package com.company.finance.api.controller;

import com.company.finance.common.dto.AuditLogQuery;
import com.company.finance.domain.entity.AuditLog;
import com.company.finance.service.audit.AuditLogSearchService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuditController 单元测试
 */
class AuditControllerTest {

    private AuditLogSearchService auditLogSearchService;
    private AuditController auditController;

    @BeforeEach
    void setUp() {
        auditLogSearchService = mock(AuditLogSearchService.class);
        auditController = new AuditController(auditLogSearchService);
    }

    @Test
    void queryAuditLogsShouldReturnFilteredLogsByEmployeeId() {
        AuditLog log1 = AuditLog.builder()
                .logId("log-001").employeeId("EMP001").action("CHAT")
                .requestContent("报销查询").timestamp(LocalDateTime.now()).build();

        when(auditLogSearchService.search(any(AuditLogQuery.class)))
                .thenReturn(Collections.singletonList(log1));

        Mono<ResponseEntity<List<AuditLog>>> result =
                auditController.queryAuditLogs(null, null, "EMP001", null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).hasSize(1);
                    assertThat(response.getBody().get(0).getEmployeeId()).isEqualTo("EMP001");
                })
                .verifyComplete();
    }

    @Test
    void queryAuditLogsShouldReturnEmptyListWhenNoMatch() {
        when(auditLogSearchService.search(any(AuditLogQuery.class)))
                .thenReturn(Collections.emptyList());

        Mono<ResponseEntity<List<AuditLog>>> result =
                auditController.queryAuditLogs(null, null, null, null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void queryAuditLogsShouldPassAllParametersToService() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59, 59);

        when(auditLogSearchService.search(any(AuditLogQuery.class)))
                .thenReturn(Collections.emptyList());

        auditController.queryAuditLogs(start, end, "EMP002", "sess-001");

        ArgumentCaptor<AuditLogQuery> captor = ArgumentCaptor.forClass(AuditLogQuery.class);
        verify(auditLogSearchService).search(captor.capture());

        AuditLogQuery captured = captor.getValue();
        assertThat(captured.getStartTime()).isEqualTo(start);
        assertThat(captured.getEndTime()).isEqualTo(end);
        assertThat(captured.getEmployeeId()).isEqualTo("EMP002");
        assertThat(captured.getSessionId()).isEqualTo("sess-001");
    }

    @Test
    void queryAuditLogsShouldReturnMultipleLogs() {
        AuditLog log1 = AuditLog.builder()
                .logId("log-001").employeeId("EMP001").action("CHAT")
                .timestamp(LocalDateTime.now()).build();
        AuditLog log2 = AuditLog.builder()
                .logId("log-002").employeeId("EMP001").action("TOOL_CALL")
                .timestamp(LocalDateTime.now()).build();

        when(auditLogSearchService.search(any(AuditLogQuery.class)))
                .thenReturn(Arrays.asList(log1, log2));

        Mono<ResponseEntity<List<AuditLog>>> result =
                auditController.queryAuditLogs(null, null, "EMP001", null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).hasSize(2);
                })
                .verifyComplete();
    }

    @Test
    void queryAuditLogsShouldFilterBySessionId() {
        AuditLog log1 = AuditLog.builder()
                .logId("log-003").sessionId("sess-abc").employeeId("EMP003")
                .action("CHAT").timestamp(LocalDateTime.now()).build();

        when(auditLogSearchService.search(any(AuditLogQuery.class)))
                .thenReturn(Collections.singletonList(log1));

        Mono<ResponseEntity<List<AuditLog>>> result =
                auditController.queryAuditLogs(null, null, null, "sess-abc");

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).hasSize(1);
                    assertThat(response.getBody().get(0).getSessionId()).isEqualTo("sess-abc");
                })
                .verifyComplete();

        ArgumentCaptor<AuditLogQuery> captor = ArgumentCaptor.forClass(AuditLogQuery.class);
        verify(auditLogSearchService).search(captor.capture());
        assertThat(captor.getValue().getSessionId()).isEqualTo("sess-abc");
    }

    @Test
    void queryAuditLogsShouldFilterByTimeRange() {
        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 6, 30, 23, 59, 59);

        AuditLog log1 = AuditLog.builder()
                .logId("log-004").employeeId("EMP004").action("CHAT")
                .timestamp(LocalDateTime.of(2024, 6, 15, 10, 0)).build();

        when(auditLogSearchService.search(any(AuditLogQuery.class)))
                .thenReturn(Collections.singletonList(log1));

        Mono<ResponseEntity<List<AuditLog>>> result =
                auditController.queryAuditLogs(start, end, null, null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).hasSize(1);
                })
                .verifyComplete();

        ArgumentCaptor<AuditLogQuery> captor = ArgumentCaptor.forClass(AuditLogQuery.class);
        verify(auditLogSearchService).search(captor.capture());
        assertThat(captor.getValue().getStartTime()).isEqualTo(start);
        assertThat(captor.getValue().getEndTime()).isEqualTo(end);
    }
}
