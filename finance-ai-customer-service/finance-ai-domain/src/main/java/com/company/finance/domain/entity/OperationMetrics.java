package com.company.finance.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 运营指标实体
 * <p>
 * 按日统计的运营数据，包含服务量、自助解决率、
 * 人工转接率、平均响应时间、满意度评分和热点问题。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationMetrics {

    /** 指标唯一标识 */
    private String metricId;

    /** 统计日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /** 总会话数 */
    private long totalSessions;

    /** 自助解决会话数 */
    private long selfResolvedSessions;

    /** 人工转接会话数 */
    private long humanHandoffSessions;

    /** 平均响应时间（毫秒） */
    private double avgResponseTimeMs;

    /** 平均满意度评分 */
    private double satisfactionScore;

    /** 热点问题统计：问题描述 → 出现次数 */
    @Builder.Default
    private Map<String, Long> hotTopics = new HashMap<>();

    /**
     * 基于 metricId 判断相等性，支持序列化往返比较
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OperationMetrics that = (OperationMetrics) o;
        return metricId != null && metricId.equals(that.metricId);
    }

    @Override
    public int hashCode() {
        return metricId != null ? metricId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "OperationMetrics{" +
                "metricId='" + metricId + '\'' +
                ", date=" + date +
                ", totalSessions=" + totalSessions +
                ", selfResolvedSessions=" + selfResolvedSessions +
                ", humanHandoffSessions=" + humanHandoffSessions +
                ", avgResponseTimeMs=" + avgResponseTimeMs +
                ", satisfactionScore=" + satisfactionScore +
                '}';
    }
}
