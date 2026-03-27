package com.company.finance.service.operation;

import com.company.finance.common.dto.AuditLogQuery;
import com.company.finance.domain.entity.AuditLog;
import com.company.finance.domain.entity.OperationMetrics;
import com.company.finance.infrastructure.mapper.AuditLogMapper;
import com.company.finance.infrastructure.mapper.OperationMetricsMapper;
import com.company.finance.infrastructure.mapper.SatisfactionFeedbackMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OperationService 单元测试
 * <p>
 * 验证热点问题统计、运营指标计算和自助解决率告警检测。
 * </p>
 */
class OperationServiceTest {

    private AuditLogMapper auditLogMapper;
    private OperationMetricsMapper operationMetricsMapper;
    private SatisfactionFeedbackMapper satisfactionFeedbackMapper;
    private OperationService operationService;

    @BeforeEach
    void setUp() {
        auditLogMapper = mock(AuditLogMapper.class);
        operationMetricsMapper = mock(OperationMetricsMapper.class);
        satisfactionFeedbackMapper = mock(SatisfactionFeedbackMapper.class);
        operationService = new OperationService(auditLogMapper, operationMetricsMapper, satisfactionFeedbackMapper);
    }

    // ========== 热点问题统计测试 ==========

    @Test
    @DisplayName("calculateHotTopics 应按频次降序返回前 20 个高频问题")
    void shouldReturnTop20HotTopicsSortedByFrequency() {
        List<AuditLog> logs = new ArrayList<>();
        // 创建不同频次的问题
        for (int i = 0; i < 10; i++) logs.add(buildLog("sess-1", "报销标准查询"));
        for (int i = 0; i < 8; i++) logs.add(buildLog("sess-2", "发票验真"));
        for (int i = 0; i < 5; i++) logs.add(buildLog("sess-3", "工资查询"));

        List<OperationService.HotTopic> result = operationService.calculateHotTopics(logs);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getTopic()).isEqualTo("报销标准查询");
        assertThat(result.get(0).getCount()).isEqualTo(10);
        assertThat(result.get(1).getTopic()).isEqualTo("发票验真");
        assertThat(result.get(1).getCount()).isEqualTo(8);
        assertThat(result.get(2).getTopic()).isEqualTo("工资查询");
        assertThat(result.get(2).getCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("calculateHotTopics 应限制最多返回 20 条")
    void shouldLimitHotTopicsTo20() {
        List<AuditLog> logs = new ArrayList<>();
        // 创建 25 个不同的问题
        for (int i = 0; i < 25; i++) {
            for (int j = 0; j < (25 - i); j++) {
                logs.add(buildLog("sess-" + i, "问题" + i));
            }
        }

        List<OperationService.HotTopic> result = operationService.calculateHotTopics(logs);

        assertThat(result).hasSize(20);
        // 验证排序：第一个应该是频次最高的
        assertThat(result.get(0).getCount()).isGreaterThanOrEqualTo(result.get(1).getCount());
    }

    @Test
    @DisplayName("calculateHotTopics 空日志应返回空列表")
    void shouldReturnEmptyListForEmptyLogs() {
        List<OperationService.HotTopic> result = operationService.calculateHotTopics(Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("calculateHotTopics null 日志应返回空列表")
    void shouldReturnEmptyListForNullLogs() {
        List<OperationService.HotTopic> result = operationService.calculateHotTopics((List<AuditLog>) null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("calculateHotTopics 应过滤掉 requestContent 为空的日志")
    void shouldFilterOutLogsWithEmptyRequestContent() {
        List<AuditLog> logs = Arrays.asList(
                buildLog("sess-1", "报销查询"),
                buildLog("sess-2", null),
                buildLog("sess-3", ""),
                buildLog("sess-4", "报销查询")
        );

        List<OperationService.HotTopic> result = operationService.calculateHotTopics(logs);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTopic()).isEqualTo("报销查询");
        assertThat(result.get(0).getCount()).isEqualTo(2);
    }

    // ========== 运营指标计算测试 ==========

    @Test
    @DisplayName("calculateMetrics 应正确计算总会话数、自助解决数和人工转接数")
    void shouldCalculateMetricsCorrectly() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        when(operationMetricsMapper.selectByDate(date)).thenReturn(null);

        List<AuditLog> logs = Arrays.asList(
                buildLogWithAction("sess-1", "EMP001", "CHAT"),
                buildLogWithAction("sess-2", "EMP002", "CHAT"),
                buildLogWithAction("sess-3", "EMP003", "CHAT"),
                buildLogWithAction("sess-3", "EMP003", "HANDOFF"),
                buildLogWithAction("sess-4", "EMP004", "CHAT"),
                buildLogWithAction("sess-4", "EMP004", "HANDOFF")
        );
        when(auditLogMapper.selectByCondition(any(AuditLogQuery.class))).thenReturn(logs);
        when(satisfactionFeedbackMapper.selectAvgScoreByDateRange(date, date)).thenReturn(4.5);
        when(operationMetricsMapper.insertOrUpdate(any(), any())).thenReturn(1);

        OperationMetrics metrics = operationService.calculateMetrics(date);

        assertThat(metrics.getTotalSessions()).isEqualTo(4);
        assertThat(metrics.getHumanHandoffSessions()).isEqualTo(2);
        assertThat(metrics.getSelfResolvedSessions()).isEqualTo(2);
        assertThat(metrics.getSatisfactionScore()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("calculateMetrics 已有缓存指标时应直接返回")
    void shouldReturnCachedMetricsIfExists() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        OperationMetrics existing = OperationMetrics.builder()
                .metricId("existing-id")
                .date(date)
                .totalSessions(100)
                .build();
        when(operationMetricsMapper.selectByDate(date)).thenReturn(existing);

        OperationMetrics result = operationService.calculateMetrics(date);

        assertThat(result.getMetricId()).isEqualTo("existing-id");
        assertThat(result.getTotalSessions()).isEqualTo(100);
        // 不应查询审计日志
        verify(auditLogMapper, never()).selectByCondition(any());
    }

    @Test
    @DisplayName("calculateMetrics 无满意度数据时评分应为 0")
    void shouldDefaultSatisfactionScoreToZero() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        when(operationMetricsMapper.selectByDate(date)).thenReturn(null);
        when(auditLogMapper.selectByCondition(any())).thenReturn(Collections.emptyList());
        when(satisfactionFeedbackMapper.selectAvgScoreByDateRange(date, date)).thenReturn(null);
        when(operationMetricsMapper.insertOrUpdate(any(), any())).thenReturn(1);

        OperationMetrics metrics = operationService.calculateMetrics(date);

        assertThat(metrics.getSatisfactionScore()).isEqualTo(0.0);
    }

    // ========== 自助解决率告警测试 ==========

    @Test
    @DisplayName("checkSelfResolveRateAlert 低于阈值时应返回 true")
    void shouldAlertWhenSelfResolveRateBelowThreshold() {
        operationService.setAlertThreshold(0.8);
        OperationMetrics metrics = OperationMetrics.builder()
                .totalSessions(100)
                .selfResolvedSessions(70) // 70% < 80%
                .build();

        assertThat(operationService.checkSelfResolveRateAlert(metrics)).isTrue();
    }

    @Test
    @DisplayName("checkSelfResolveRateAlert 高于阈值时应返回 false")
    void shouldNotAlertWhenSelfResolveRateAboveThreshold() {
        operationService.setAlertThreshold(0.8);
        OperationMetrics metrics = OperationMetrics.builder()
                .totalSessions(100)
                .selfResolvedSessions(90) // 90% > 80%
                .build();

        assertThat(operationService.checkSelfResolveRateAlert(metrics)).isFalse();
    }

    @Test
    @DisplayName("checkSelfResolveRateAlert 等于阈值时应返回 false")
    void shouldNotAlertWhenSelfResolveRateEqualsThreshold() {
        operationService.setAlertThreshold(0.8);
        OperationMetrics metrics = OperationMetrics.builder()
                .totalSessions(100)
                .selfResolvedSessions(80) // 80% == 80%
                .build();

        assertThat(operationService.checkSelfResolveRateAlert(metrics)).isFalse();
    }

    @Test
    @DisplayName("checkSelfResolveRateAlert 总会话数为 0 时应返回 false")
    void shouldNotAlertWhenTotalSessionsIsZero() {
        OperationMetrics metrics = OperationMetrics.builder()
                .totalSessions(0)
                .selfResolvedSessions(0)
                .build();

        assertThat(operationService.checkSelfResolveRateAlert(metrics)).isFalse();
    }

    @Test
    @DisplayName("checkSelfResolveRateAlert null 指标应返回 false")
    void shouldNotAlertWhenMetricsIsNull() {
        assertThat(operationService.checkSelfResolveRateAlert(null)).isFalse();
    }

    @Test
    @DisplayName("setAlertThreshold 应更新告警阈值")
    void shouldUpdateAlertThreshold() {
        operationService.setAlertThreshold(0.6);
        assertThat(operationService.getAlertThreshold()).isEqualTo(0.6);

        // 60% 自助解决率，阈值 60%，不应告警
        OperationMetrics metrics = OperationMetrics.builder()
                .totalSessions(100)
                .selfResolvedSessions(60)
                .build();
        assertThat(operationService.checkSelfResolveRateAlert(metrics)).isFalse();

        // 50% 自助解决率，阈值 60%，应告警
        OperationMetrics metrics2 = OperationMetrics.builder()
                .totalSessions(100)
                .selfResolvedSessions(50)
                .build();
        assertThat(operationService.checkSelfResolveRateAlert(metrics2)).isTrue();
    }

    // ========== getMetricsByDateRange 测试 ==========

    @Test
    @DisplayName("getMetricsByDateRange 应委托给 Mapper 查询")
    void shouldDelegateToMapperForDateRange() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        List<OperationMetrics> expected = Arrays.asList(
                OperationMetrics.builder().metricId("m1").date(start).build(),
                OperationMetrics.builder().metricId("m2").date(end).build()
        );
        when(operationMetricsMapper.selectByDateRange(start, end)).thenReturn(expected);

        List<OperationMetrics> result = operationService.getMetricsByDateRange(start, end);

        assertThat(result).hasSize(2);
        verify(operationMetricsMapper).selectByDateRange(start, end);
    }

    // ========== 辅助方法 ==========

    private AuditLog buildLog(String sessionId, String requestContent) {
        return AuditLog.builder()
                .logId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .employeeId("EMP001")
                .action("CHAT")
                .requestContent(requestContent)
                .responseContent("response")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private AuditLog buildLogWithAction(String sessionId, String employeeId, String action) {
        return AuditLog.builder()
                .logId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .employeeId(employeeId)
                .action(action)
                .requestContent("test request")
                .responseContent("test response")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
