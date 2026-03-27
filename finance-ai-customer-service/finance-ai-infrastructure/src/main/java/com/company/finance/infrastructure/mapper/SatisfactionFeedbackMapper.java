package com.company.finance.infrastructure.mapper;

import com.company.finance.domain.entity.SatisfactionFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 满意度反馈 Mapper 接口
 * <p>
 * 提供满意度反馈的插入和按日期范围统计平均评分操作。
 * </p>
 */
@Mapper
public interface SatisfactionFeedbackMapper {

    /**
     * 插入满意度反馈
     *
     * @param feedback 满意度反馈实体
     * @return 影响行数
     */
    int insert(SatisfactionFeedback feedback);

    /**
     * 查询指定日期范围内的平均满意度评分
     *
     * @param start 起始日期（包含）
     * @param end 结束日期（包含）
     * @return 平均评分，无数据时返回 null
     */
    Double selectAvgScoreByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * 根据会话 ID 查询满意度反馈
     *
     * @param sessionId 会话 ID
     * @return 满意度反馈实体，不存在时返回 null
     */
    SatisfactionFeedback selectBySessionId(@Param("sessionId") String sessionId);
}
