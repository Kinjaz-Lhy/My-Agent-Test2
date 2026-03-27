package com.company.finance.api.controller;

import com.company.finance.common.dto.AuditLogQuery;
import com.company.finance.domain.entity.AuditLog;
import com.company.finance.service.audit.AuditLogSearchService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计控制器
 * <p>
 * 提供审计日志检索端点，支持按时间范围、用户、会话 ID 多条件组合查询。
 * 所有端点需要 AUDITOR 角色（Spring Security 配置 /api/v1/audit/** → AUDITOR）。
 * </p>
 *
 * @see <a href="需求 5.5">权限与安全 — 审计日志检索</a>
 */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditLogSearchService auditLogSearchService;

    public AuditController(AuditLogSearchService auditLogSearchService) {
        this.auditLogSearchService = auditLogSearchService;
    }

    /**
     * 审计日志检索
     * <p>
     * 支持按时间范围、用户、会话 ID 多条件组合检索。
     * 所有参数均为可选，为空时不参与过滤。
     * </p>
     *
     * @param startTime  起始时间（可选）
     * @param endTime    结束时间（可选）
     * @param employeeId 员工 ID（可选）
     * @param sessionId  会话 ID（可选）
     * @return 审计日志列表
     */
    @GetMapping("/logs")
    public Mono<ResponseEntity<List<AuditLog>>> queryAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String sessionId) {
        AuditLogQuery query = AuditLogQuery.builder()
                .startTime(startTime)
                .endTime(endTime)
                .employeeId(employeeId)
                .sessionId(sessionId)
                .build();
        List<AuditLog> logs = auditLogSearchService.search(query);
        return Mono.just(ResponseEntity.ok(logs));
    }
}
