package com.company.finance.infrastructure.mapper;

import com.company.finance.common.dto.AuditLogQuery;
import com.company.finance.domain.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 审计日志 Mapper 接口
 * <p>
 * 提供审计日志的插入和多条件组合查询操作。
 * 支持按时间范围、employeeId、intent、sessionId 动态组合查询。
 * </p>
 */
@Mapper
public interface AuditLogMapper {

    /**
     * 插入审计日志
     *
     * @param auditLog 审计日志实体
     * @return 影响行数
     */
    int insert(AuditLog auditLog);

    /**
     * 多条件组合查询审计日志
     * <p>
     * 所有条件均为可选，为空时不参与过滤。
     * 结果按时间戳降序排列。
     * </p>
     *
     * @param query 查询条件 DTO
     * @return 审计日志列表
     */
    List<AuditLog> selectByCondition(@Param("query") AuditLogQuery query);
}
