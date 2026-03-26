package com.company.finance.domain.repository;

import com.company.finance.domain.entity.OperationMetrics;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 运营指标仓储接口
 * <p>
 * 定义运营指标的持久化和查询操作，由基础设施层实现。
 * </p>
 */
public interface OperationMetricsRepository {

    /**
     * 保存或更新运营指标（按日期唯一）
     *
     * @param metrics 运营指标实体
     */
    void saveOrUpdate(OperationMetrics metrics);

    /**
     * 根据日期查询运营指标
     *
     * @param date 统计日期
     * @return 运营指标（可能为空）
     */
    Optional<OperationMetrics> findByDate(LocalDate date);

    /**
     * 根据日期范围查询运营指标
     *
     * @param start 起始日期（包含）
     * @param end   结束日期（包含）
     * @return 运营指标列表
     */
    List<OperationMetrics> findByDateRange(LocalDate start, LocalDate end);
}
