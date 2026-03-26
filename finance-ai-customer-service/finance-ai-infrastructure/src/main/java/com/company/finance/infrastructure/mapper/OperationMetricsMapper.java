package com.company.finance.infrastructure.mapper;

import com.company.finance.domain.entity.OperationMetrics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 运营指标 Mapper 接口
 * <p>
 * 提供运营指标的插入/更新、按日期查询和按日期范围查询操作。
 * hotTopics 字段（Map 类型）在数据库中以 hot_topics_json（TEXT）形式存储，
 * Mapper 层以 String 形式处理 JSON，序列化/反序列化由上层服务处理。
 * </p>
 */
@Mapper
public interface OperationMetricsMapper {

    /**
     * 插入或更新运营指标（基于 metric_date 唯一键）
     * <p>
     * 使用 MySQL ON DUPLICATE KEY UPDATE 语法，
     * 若 metric_date 已存在则更新，否则插入新记录。
     * </p>
     *
     * @param metrics 运营指标实体
     * @param hotTopicsJson 热点问题 JSON 字符串
     * @return 影响行数
     */
    int insertOrUpdate(@Param("metrics") OperationMetrics metrics, @Param("hotTopicsJson") String hotTopicsJson);

    /**
     * 根据日期查询运营指标
     *
     * @param date 统计日期
     * @return 运营指标实体
     */
    OperationMetrics selectByDate(@Param("date") LocalDate date);

    /**
     * 根据日期范围查询运营指标列表
     *
     * @param start 起始日期（包含）
     * @param end 结束日期（包含）
     * @return 运营指标列表
     */
    List<OperationMetrics> selectByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
