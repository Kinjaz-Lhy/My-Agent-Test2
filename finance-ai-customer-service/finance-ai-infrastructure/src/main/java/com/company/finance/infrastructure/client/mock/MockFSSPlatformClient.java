package com.company.finance.infrastructure.client.mock;

import com.company.finance.infrastructure.client.FSSPlatformClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 财务共享平台客户端 Mock 实现
 * <p>
 * 用于开发和测试环境，返回模拟数据，不调用真实外部系统。
 * </p>
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class MockFSSPlatformClient implements FSSPlatformClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, Object> queryExpenseStatus(String expenseId, String employeeId) {
        log.info("[Mock-FSS] 查询报销单状态: expenseId={}, employeeId={}", expenseId, employeeId);
        Map<String, Object> result = new HashMap<>();
        result.put("expenseId", expenseId);
        result.put("employeeId", employeeId);
        result.put("status", "APPROVED");
        result.put("amount", 3500.00);
        result.put("submittedAt", LocalDateTime.now().minusDays(3).format(FORMATTER));
        result.put("approvedAt", LocalDateTime.now().minusDays(1).format(FORMATTER));
        result.put("currentStep", "财务审核");
        result.put("description", "差旅报销-北京出差");
        return result;
    }

    @Override
    public Map<String, Object> submitExpense(Map<String, Object> expenseData) {
        log.info("[Mock-FSS] 提交报销单: {}", expenseData);
        Map<String, Object> result = new HashMap<>();
        result.put("expenseId", "EXP-" + System.currentTimeMillis());
        result.put("status", "SUBMITTED");
        result.put("submittedAt", LocalDateTime.now().format(FORMATTER));
        result.put("message", "报销单提交成功，等待审批");
        return result;
    }

    @Override
    public Map<String, Object> queryLoanStatus(String loanId, String employeeId) {
        log.info("[Mock-FSS] 查询借款单状态: loanId={}, employeeId={}", loanId, employeeId);
        Map<String, Object> result = new HashMap<>();
        result.put("loanId", loanId);
        result.put("employeeId", employeeId);
        result.put("status", "PENDING_APPROVAL");
        result.put("amount", 10000.00);
        result.put("submittedAt", LocalDateTime.now().minusDays(1).format(FORMATTER));
        result.put("purpose", "项目预付款");
        return result;
    }

    @Override
    public Map<String, Object> submitBilling(Map<String, Object> billingData) {
        log.info("[Mock-FSS] 提交开票申请: {}", billingData);
        Map<String, Object> result = new HashMap<>();
        result.put("billingId", "BIL-" + System.currentTimeMillis());
        result.put("status", "SUBMITTED");
        result.put("submittedAt", LocalDateTime.now().format(FORMATTER));
        result.put("message", "开票申请提交成功");
        return result;
    }
}
