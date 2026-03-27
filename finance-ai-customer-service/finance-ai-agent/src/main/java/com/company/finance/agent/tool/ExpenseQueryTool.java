package com.company.finance.agent.tool;

import com.company.finance.infrastructure.client.FSSPlatformClient;
import kd.ai.nova.core.tool.annotation.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 报销单状态查询工具
 * <p>
 * 通过财务共享平台查询报销单的当前状态、审批进度等信息。
 * </p>
 */
@Slf4j
@Component
public class ExpenseQueryTool {

    private final FSSPlatformClient fssPlatformClient;

    public ExpenseQueryTool(FSSPlatformClient fssPlatformClient) {
        this.fssPlatformClient = fssPlatformClient;
    }

    @Tool(description = "根据报销单号和员工ID查询报销单状态，返回报销单当前审批状态、金额、提交时间等信息")
    public String queryExpenseStatus(String expenseId, String employeeId) {
        try {
            log.info("查询报销单状态: expenseId={}, employeeId={}", expenseId, employeeId);
            Map<String, Object> result = fssPlatformClient.queryExpenseStatus(expenseId, employeeId);
            return formatResult(result);
        } catch (Exception e) {
            log.error("查询报销单状态失败: expenseId={}, employeeId={}", expenseId, employeeId, e);
            return "查询报销单状态失败，请稍后重试或联系人工客服。";
        }
    }

    private String formatResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("报销单查询结果：\n");
        sb.append("- 报销单号: ").append(result.getOrDefault("expenseId", "未知")).append("\n");
        sb.append("- 状态: ").append(result.getOrDefault("status", "未知")).append("\n");
        sb.append("- 金额: ").append(result.getOrDefault("amount", "未知")).append("\n");
        sb.append("- 当前步骤: ").append(result.getOrDefault("currentStep", "未知")).append("\n");
        sb.append("- 描述: ").append(result.getOrDefault("description", "无")).append("\n");
        sb.append("- 提交时间: ").append(result.getOrDefault("submittedAt", "未知")).append("\n");
        sb.append("- 审批时间: ").append(result.getOrDefault("approvedAt", "暂无"));
        return sb.toString();
    }
}
