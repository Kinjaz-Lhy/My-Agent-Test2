package com.company.finance.domain.repository;

import com.company.finance.common.dto.AuditLogQuery;
import com.company.finance.domain.entity.AuditLog;

import java.util.List;

/**
 * 审计日志仓储接口
 * <p>
 * 定义审计日志的持久化和检索操作，由基础设施层实现。
 * </p>
 */
public interface AuditLogRepository {

    /**
     * 保存审计日志
     *
     * @param auditLog 审计日志实体
     */
    void save(AuditLog auditLog);

    /**
     * 按条件组合查询审计日志
     * <p>
     * 支持按时间范围、员工 ID、意图分类、会话 ID 多条件组合过滤。
     * </p>
     *
     * @param query 查询条件
     * @return 匹配的审计日志列表
     */
    List<AuditLog> findByCondition(AuditLogQuery query);
}
