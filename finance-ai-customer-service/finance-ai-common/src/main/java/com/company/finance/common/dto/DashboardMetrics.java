package com.company.finance.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 运营指标看板 DTO
 * <p>
 * 包含前端看板所需的计算后指标和满意度趋势数据。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardMetrics {

    /** 今日服务量 */
    private long totalSessions;

    /** 自助解决率（0.0 ~ 1.0） */
    private Double selfResolveRate;

    /** 人工转接率（0.0 ~ 1.0） */
    private Double handoffRate;

    /** 平均响应时间（毫秒） */
    private Double avgResponseTimeMs;

    /** 满意度趋势（近 7 天） */
    private List<SatisfactionTrendItem> satisfactionTrend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SatisfactionTrendItem {
        private String date;
        private Double score;
    }
}
