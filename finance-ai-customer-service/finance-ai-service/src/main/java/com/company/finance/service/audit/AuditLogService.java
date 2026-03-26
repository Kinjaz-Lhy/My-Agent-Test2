package com.company.finance.service.audit;

import com.company.finance.domain.entity.AuditLog;
import com.company.finance.infrastructure.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审计日志服务
 * <p>
 * 负责记录对话交互的完整审计日志，包含时间戳、用户身份、
 * 请求内容、响应内容和脱敏后的响应内容，并通过 AuditLogMapper 持久化到数据库。
 * </p>
 *
 * @see AuditLog
 * @see AuditLogMapper
 */
@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 记录对话审计日志
     * <p>
     * 生成唯一 logId（UUID），记录当前时间戳，构建 AuditLog 实体并持久化。
     * </p>
     *
     * @param sessionId             会话 ID
     * @param employeeId            员工 ID（用户身份）
     * @param action                操作类型（CHAT / TOOL_CALL / HANDOFF / LOGIN）
     * @param requestContent        请求内容
     * @param responseContent       响应内容
     * @param maskedResponseContent 脱敏后的响应内容
     */
    public void logConversation(String sessionId,
                                String employeeId,
                                String action,
                                String requestContent,
                                String responseContent,
                                String maskedResponseContent) {
        AuditLog auditLog = AuditLog.builder()
                .logId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .employeeId(employeeId)
                .action(action)
                .requestContent(requestContent)
                .responseContent(responseContent)
                .maskedResponseContent(maskedResponseContent)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogMapper.insert(auditLog);
    }
}
