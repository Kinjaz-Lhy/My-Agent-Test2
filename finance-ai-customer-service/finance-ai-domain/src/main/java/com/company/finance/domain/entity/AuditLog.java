package com.company.finance.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 * <p>
 * 记录所有对话交互的完整日志，包含时间戳、用户身份、
 * 请求内容、响应内容和脱敏后的响应内容，用于合规审计。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLog {

    /** 日志唯一标识 */
    private String logId;

    /** 会话 ID */
    private String sessionId;

    /** 员工 ID */
    private String employeeId;

    /** 操作类型：CHAT / TOOL_CALL / HANDOFF / LOGIN */
    private String action;

    /** 请求内容 */
    private String requestContent;

    /** 响应内容 */
    private String responseContent;

    /** 脱敏后的响应内容 */
    private String maskedResponseContent;

    /** 时间戳 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 基于 logId 判断相等性，支持序列化往返比较
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return logId != null && logId.equals(auditLog.logId);
    }

    @Override
    public int hashCode() {
        return logId != null ? logId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "logId='" + logId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
