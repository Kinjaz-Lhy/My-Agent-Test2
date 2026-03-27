package com.company.finance.service.audit;

import com.company.finance.common.dto.AuditLogQuery;
import com.company.finance.domain.entity.AuditLog;
import com.company.finance.infrastructure.mapper.AuditLogMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 日志检索服务
 * <p>
 * 支持按时间范围、用户、意图分类、会话 ID 多条件组合检索审计日志。
 * 所有条件均为可选，为空时不参与过滤。
 * </p>
 *
 * @see <a href="需求 5.5, 6.1">权限与安全 &amp; 运营管理</a>
 */
@Service
public class AuditLogSearchService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogSearchService.class);

    private final AuditLogMapper auditLogMapper;

    public AuditLogSearchService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 多条件组合检索审计日志
     * <p>
     * 支持按时间范围、用户、意图分类、会话 ID 任意组合查询。
     * 所有条件均为可选，为空时不参与过滤。
     * 结果按时间戳降序排列。
     * </p>
     *
     * @param query 查询条件 DTO
     * @return 审计日志列表
     */
    public List<AuditLog> search(AuditLogQuery query) {
        if (query == null) {
            query = new AuditLogQuery();
        }
        log.debug("检索审计日志: startTime={}, endTime={}, employeeId={}, intent={}, sessionId={}",
                query.getStartTime(), query.getEndTime(),
                query.getEmployeeId(), query.getIntent(), query.getSessionId());

        List<AuditLog> results = auditLogMapper.selectByCondition(query);
        return results != null ? results : Collections.emptyList();
    }

    /**
     * 按时间范围检索
     *
     * @param startTime 起始时间（包含）
     * @param endTime   结束时间（包含）
     * @return 审计日志列表
     */
    public List<AuditLog> searchByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        AuditLogQuery query = AuditLogQuery.builder()
                .startTime(startTime)
                .endTime(endTime)
                .build();
        return search(query);
    }

    /**
     * 按员工 ID 检索
     *
     * @param employeeId 员工 ID
     * @return 审计日志列表
     */
    public List<AuditLog> searchByEmployee(String employeeId) {
        AuditLogQuery query = AuditLogQuery.builder()
                .employeeId(employeeId)
                .build();
        return search(query);
    }

    /**
     * 按意图分类检索
     *
     * @param intent 意图分类
     * @return 审计日志列表
     */
    public List<AuditLog> searchByIntent(String intent) {
        AuditLogQuery query = AuditLogQuery.builder()
                .intent(intent)
                .build();
        return search(query);
    }

    /**
     * 按会话 ID 检索
     *
     * @param sessionId 会话 ID
     * @return 审计日志列表
     */
    public List<AuditLog> searchBySessionId(String sessionId) {
        AuditLogQuery query = AuditLogQuery.builder()
                .sessionId(sessionId)
                .build();
        return search(query);
    }
}
