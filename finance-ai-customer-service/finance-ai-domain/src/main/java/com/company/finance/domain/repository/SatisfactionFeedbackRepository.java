package com.company.finance.domain.repository;

import com.company.finance.domain.entity.SatisfactionFeedback;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 满意度评价仓储接口
 * <p>
 * 定义满意度评价的持久化和统计操作，由基础设施层实现。
 * </p>
 */
public interface SatisfactionFeedbackRepository {

    /**
     * 保存满意度评价
     *
     * @param feedback 满意度评价实体
     */
    void save(SatisfactionFeedback feedback);

    /**
     * 查询指定日期范围内的平均满意度评分
     *
     * @param start 起始日期（包含）
     * @param end   结束日期（包含）
     * @return 平均评分（可能为空，表示无数据）
     */
    Optional<Double> findAvgScoreByDateRange(LocalDate start, LocalDate end);
}
