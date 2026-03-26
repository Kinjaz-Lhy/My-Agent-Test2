package com.company.finance.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志查询条件 DTO
 * <p>
 * 支持按时间范围、用户、意图分类、会话 ID 多条件组合查询。
 * 所有条件均为可选，为空时不参与过滤。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogQuery {

    /** 查询起始时间（包含） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 查询结束时间（包含） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 员工 ID */
    private String employeeId;

    /** 意图分类（如 EXPENSE_QUERY、INVOICE_VERIFY 等） */
    private String intent;

    /** 会话 ID */
    private String sessionId;
}
