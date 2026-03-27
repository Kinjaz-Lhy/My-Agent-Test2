package com.company.finance.service.audit;

import com.company.finance.common.dto.AuditLogQuery;
import com.company.finance.domain.entity.AuditLog;
import com.company.finance.infrastructure.mapper.AuditLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuditLogSearchService 单元测试
 * <p>
 * 验证日志检索服务的多条件组合检索逻辑。
 * </p>
 */
class AuditLogSearchServiceTest {

    private AuditLogMapper auditLogMapper;
    private AuditLogSearchService auditLogSearchService;

    @BeforeEach
    void setUp() {
        auditLogMapper = mock(AuditLogMapper.class);
        auditLogSearchService = new AuditLogSearchService(auditLogMapper);
    }

    @Test
    @DisplayName("search 应将查询条件传递给 Mapper 并返回结果")
    void searchShouldDelegateToMapper() {
        AuditLogQuery query = AuditLogQuery.builder()
                .employeeId("EMP001")
                .sessionId("sess-001")
                .build();
        List<AuditLog> expected = Arrays.asList(
                buildLog("log-1", "sess-001", "EMP001", "CHAT"),
                buildLog("log-2", "sess-001", "EMP001", "TOOL_CALL")
        );
        when(auditLogMapper.selectByCondition(any(AuditLogQuery.class))).thenReturn(expected);

        List<AuditLog> result = auditLogSearchService.search(query);

        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expected);
        verify(auditLogMapper).selectByCondition(query);
    }

    @Test
    @DisplayName("search 传入 null 查询条件时应使用空查询并返回结果")
    void searchWithNullQueryShouldUseEmptyQuery() {
        List<AuditLog> expected = Collections.singletonList(
                buildLog("log-1", "sess-001", "EMP001", "CHAT")
        );
        when(auditLogMapper.selectByCondition(any(AuditLogQuery.class))).thenReturn(expected);

        List<AuditLog> result = auditLogSearchService.search(null);

        assertThat(result).hasSize(1);
        ArgumentCaptor<AuditLogQuery> captor = ArgumentCaptor.forClass(AuditLogQuery.class);
        verify(auditLogMapper).selectByCondition(captor.capture());
        AuditLogQuery captured = captor.getValue();
        assertThat(captured.getStartTime()).isNull();
        assertThat(captured.getEndTime()).isNull();
        assertThat(captured.getEmployeeId()).isNull();
        assertThat(captured.getIntent()).isNull();
        assertThat(captured.getSessionId()).isNull();
    }

    @Test
    @DisplayName("search Mapper 返回 null 时应返回空列表")
    void searchShouldReturnEmptyListWhenMapperReturnsNull() {
        when(auditLogMapper.selectByCondition(any(AuditLogQuery.class))).thenReturn(null);

        List<AuditLog> result = auditLogSearchService.search(new AuditLogQuery());

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("searchByTimeRange 应构建正确的时间范围查询")
    void searchByTimeRangeShouldBuildCorrectQuery() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59, 59);
        when(auditLogMapper.selectByCondition(any(AuditLogQuery.class))).thenReturn(Collections.emptyList());

        auditLogSearchService.searchByTimeRange(start, end);

        ArgumentCaptor<AuditLogQuery> captor = ArgumentCaptor.forClass(AuditLogQuery.class);
        verify(auditLogMapper).selectByCondition(captor.capture());
        AuditLogQuery captured = captor.getValue();
        assertThat(captured.getStartTime()).isEqualTo(start);
        assertThat(captured.getEndTime()).isEqualTo(end);
        assertThat(captured.getEmployeeId()).isNull();
        assertThat(captured.getIntent()).isNull();
        assertThat(captured.getSessionId()).isNull();
    }

    @Test
    @DisplayName("searchByEmployee 应构建正确的员工 ID 查询")
    void searchByEmployeeShouldBuildCorrectQuery() {
        when(auditLogMapper.selectByCondition(any(AuditLogQuery.class))).thenReturn(Collections.emptyList());

        auditLogSearchService.searchByEmployee("EMP001");

        ArgumentCaptor<AuditLogQuery> captor = ArgumentCaptor.forClass(AuditLogQuery.class);
        verify(auditLogMapper).selectByCondition(captor.capture());
        AuditLogQuery captured = captor.getValue();
        assertThat(captured.getEmployeeId()).isEqualTo("EMP001");
        assertThat(captured.getStartTime()).isNull();
        assertThat(captured.getEndTime()).isNull();
        assertThat(captured.getIntent()).isNull();
        assertThat(captured.getSessionId()).isNull();
    }

    @Test
    @DisplayName("searchByIntent 应构建正确的意图分类查询")
    void searchByIntentShouldBuildCorrectQuery() {
        when(auditLogMapper.selectByCondition(any(AuditLogQuery.class))).thenReturn(Collections.emptyList());

        auditLogSearchService.searchByIntent("EXPENSE_QUERY");

        ArgumentCaptor<AuditLogQuery> captor = ArgumentCaptor.forClass(AuditLogQuery.class);
        verify(auditLogMapper).selectByCondition(captor.capture());
        AuditLogQuery captured = captor.getValue();
        assertThat(captured.getIntent()).isEqualTo("EXPENSE_QUERY");
        assertThat(captured.getStartTime()).isNull();
        assertThat(captured.getEndTime()).isNull();
        assertThat(captured.getEmployeeId()).isNull();
        assertThat(captured.getSessionId()).isNull();
    }

    @Test
    @DisplayName("searchBySessionId 应构建正确的会话 ID 查询")
    void searchBySessionIdShouldBuildCorrectQuery() {
        when(auditLogMapper.selectByCondition(any(AuditLogQuery.class))).thenReturn(Collections.emptyList());

        auditLogSearchService.searchBySessionId("sess-001");

        ArgumentCaptor<AuditLogQuery> captor = ArgumentCaptor.forClass(AuditLogQuery.class);
        verify(auditLogMapper).selectByCondition(captor.capture());
        AuditLogQuery captured = captor.getValue();
        assertThat(captured.getSessionId()).isEqualTo("sess-001");
        assertThat(captured.getStartTime()).isNull();
        assertThat(captured.getEndTime()).isNull();
        assertThat(captured.getEmployeeId()).isNull();
        assertThat(captured.getIntent()).isNull();
    }

    @Test
    @DisplayName("search 支持多条件组合查询")
    void searchShouldSupportCombinedFilters() {
        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 6, 30, 23, 59, 59);
        AuditLogQuery query = AuditLogQuery.builder()
                .startTime(start)
                .endTime(end)
                .employeeId("EMP002")
                .intent("INVOICE_VERIFY")
                .sessionId("sess-100")
                .build();
        List<AuditLog> expected = Collections.singletonList(
                buildLog("log-5", "sess-100", "EMP002", "INVOICE_VERIFY")
        );
        when(auditLogMapper.selectByCondition(query)).thenReturn(expected);

        List<AuditLog> result = auditLogSearchService.search(query);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSessionId()).isEqualTo("sess-100");
        assertThat(result.get(0).getEmployeeId()).isEqualTo("EMP002");
        verify(auditLogMapper).selectByCondition(query);
    }

    private AuditLog buildLog(String logId, String sessionId, String employeeId, String action) {
        return AuditLog.builder()
                .logId(logId)
                .sessionId(sessionId)
                .employeeId(employeeId)
                .action(action)
                .requestContent("test request")
                .responseContent("test response")
                .maskedResponseContent("test masked response")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
