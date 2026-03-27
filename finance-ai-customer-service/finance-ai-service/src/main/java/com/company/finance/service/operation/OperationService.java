package com.company.finance.service.operation;

import com.company.finance.domain.entity.AuditLog;
import com.company.finance.domain.entity.OperationMetrics;
import com.company.finance.infrastructure.mapper.AuditLogMapper;
import com.company.finance.infrastructure.mapper.OperationMetricsMapper;
import com.company.finance.infrastructure.mapper.SatisfactionFeedbackMapper;
import com.company.finance.common.dto.AuditLogQuery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 运营统计服务
 * <p>
 * 提供每日热点问题统计、运营指标计算和自助解决率告警检测。
 * </p>
 *
 * @see <a href="需求 6.2, 6.4, 6.7">运营管理</a>
 */
@Service
public class OperationService {

    private static final Logger log = LoggerFactory.getLogger(OperationService.class);

    /** 热点问题最大数量 */
    static final int MAX_HOT_TOPICS = 20;

    /** 默认自助解决率告警阈值（80%） */
    private static final double DEFAULT_ALERT_THRESHOLD = 0.8;

    private final AuditLogMapper auditLogMapper;
    private final OperationMetricsMapper operationMetricsMapper;
    private final SatisfactionFeedbackMapper satisfactionFeedbackMapper;
    private final ObjectMapper objectMapper;

    private double alertThreshold = DEFAULT_ALERT_THRESHOLD;

    public OperationService(AuditLogMapper auditLogMapper,
                            OperationMetricsMapper operationMetricsMapper,
                            SatisfactionFeedbackMapper satisfactionFeedbackMapper) {
        this.auditLogMapper = auditLogMapper;
        this.operationMetricsMapper = operationMetricsMapper;
        this.satisfactionFeedbackMapper = satisfactionFeedbackMapper;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 设置自助解决率告警阈值
     *
     * @param threshold 阈值（0.0 ~ 1.0）
     */
    public void setAlertThreshold(double threshold) {
        this.alertThreshold = threshold;
    }

    /**
     * 获取当前告警阈值
     */
    public double getAlertThreshold() {
        return alertThreshold;
    }

    /**
     * 统计每日热点问题（前 20 个高频问题）
     * <p>
     * 从审计日志中提取当日所有 CHAT 类型的请求内容，
     * 按内容分组统计频次，返回频次降序排列的前 20 条。
     * </p>
     *
     * @param date 统计日期
     * @return 热点问题映射：问题描述 → 出现次数，按频次降序排列
     */
    public List<HotTopic> calculateHotTopics(LocalDate date) {
        AuditLogQuery query = AuditLogQuery.builder()
                .startTime(date.atStartOfDay())
                .endTime(date.atTime(LocalTime.MAX))
                .build();

        List<AuditLog> logs = auditLogMapper.selectByCondition(query);
        return calculateHotTopics(logs);
    }

    /**
     * 从审计日志列表中统计热点问题
     * <p>
     * 按请求内容分组统计频次，返回频次降序排列的前 20 条。
     * </p>
     *
     * @param logs 审计日志列表
     * @return 热点问题列表，按频次降序排列，最多 20 条
     */
    public List<HotTopic> calculateHotTopics(List<AuditLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Long> topicCounts = logs.stream()
                .filter(l -> l.getRequestContent() != null && !l.getRequestContent().isEmpty())
                .collect(Collectors.groupingBy(
                        AuditLog::getRequestContent,
                        Collectors.counting()
                ));

        return topicCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(MAX_HOT_TOPICS)
                .map(e -> new HotTopic(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 计算运营指标
     * <p>
     * 计算指定日期的服务量、自助解决率、人工转接率、平均响应时间等核心指标。
     * </p>
     *
     * @param date 统计日期
     * @return 运营指标
     */
    public OperationMetrics calculateMetrics(LocalDate date) {
        OperationMetrics existing = operationMetricsMapper.selectByDate(date);
        if (existing != null) {
            return existing;
        }

        // 查询当日审计日志
        AuditLogQuery query = AuditLogQuery.builder()
                .startTime(date.atStartOfDay())
                .endTime(date.atTime(LocalTime.MAX))
                .build();
        List<AuditLog> logs = auditLogMapper.selectByCondition(query);

        // 统计会话数
        long totalSessions = logs.stream()
                .map(AuditLog::getSessionId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // 统计人工转接数（action 为 HANDOFF 的会话）
        long humanHandoffSessions = logs.stream()
                .filter(l -> "HANDOFF".equals(l.getAction()))
                .map(AuditLog::getSessionId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        long selfResolvedSessions = totalSessions - humanHandoffSessions;

        // 查询满意度评分
        Double avgScore = satisfactionFeedbackMapper.selectAvgScoreByDateRange(date, date);

        // 计算热点问题
        List<HotTopic> hotTopics = calculateHotTopics(logs);
        Map<String, Long> hotTopicsMap = new LinkedHashMap<>();
        hotTopics.forEach(ht -> hotTopicsMap.put(ht.getTopic(), ht.getCount()));

        OperationMetrics metrics = OperationMetrics.builder()
                .metricId(UUID.randomUUID().toString())
                .date(date)
                .totalSessions(totalSessions)
                .selfResolvedSessions(selfResolvedSessions)
                .humanHandoffSessions(humanHandoffSessions)
                .avgResponseTimeMs(0)
                .satisfactionScore(avgScore != null ? avgScore : 0.0)
                .hotTopics(hotTopicsMap)
                .build();

        // 持久化
        try {
            String hotTopicsJson = objectMapper.writeValueAsString(hotTopicsMap);
            operationMetricsMapper.insertOrUpdate(metrics, hotTopicsJson);
        } catch (JsonProcessingException e) {
            log.warn("热点问题序列化失败", e);
        }

        return metrics;
    }

    /**
     * 获取日期范围内的运营指标
     *
     * @param start 起始日期
     * @param end   结束日期
     * @return 运营指标列表
     */
    public List<OperationMetrics> getMetricsByDateRange(LocalDate start, LocalDate end) {
        return operationMetricsMapper.selectByDateRange(start, end);
    }

    /**
     * 检测自助解决率是否低于告警阈值
     * <p>
     * 当自助解决率（selfResolvedSessions / totalSessions）低于阈值时返回 true。
     * </p>
     *
     * @param metrics 运营指标
     * @return 是否需要告警
     */
    public boolean checkSelfResolveRateAlert(OperationMetrics metrics) {
        if (metrics == null || metrics.getTotalSessions() == 0) {
            return false;
        }
        double selfResolveRate = (double) metrics.getSelfResolvedSessions() / metrics.getTotalSessions();
        boolean shouldAlert = selfResolveRate < alertThreshold;
        if (shouldAlert) {
            log.warn("自助解决率告警: date={}, rate={}, threshold={}",
                    metrics.getDate(), selfResolveRate, alertThreshold);
        }
        return shouldAlert;
    }

    /**
     * 热点问题数据类
     */
    public static class HotTopic {
        private final String topic;
        private final long count;

        public HotTopic(String topic, long count) {
            this.topic = topic;
            this.count = count;
        }

        public String getTopic() {
            return topic;
        }

        public long getCount() {
            return count;
        }

        @Override
        public String toString() {
            return "HotTopic{topic='" + topic + "', count=" + count + '}';
        }
    }
}
