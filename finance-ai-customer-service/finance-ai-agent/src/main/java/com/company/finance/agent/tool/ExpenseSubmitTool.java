package com.company.finance.agent.tool;

import com.company.finance.infrastructure.client.FSSPlatformClient;
import kd.ai.nova.core.tool.annotation.Tool;
import kd.ai.nova.core.tool.annotation.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 报销单提交工具
 * <p>
 * 将员工填写的报销信息提交至财务共享平台。
 * </p>
 */
@Slf4j
@Component
public class ExpenseSubmitTool {

    private final FSSPlatformClient fssPlatformClient;

    public ExpenseSubmitTool(FSSPlatformClient fssPlatformClient) {
        this.fssPlatformClient = fssPlatformClient;
    }

    @Tool(description = "提交报销单到财务共享平台，需要提供报销类型、金额、事由、员工ID等信息，返回提交结果")
    public String submitExpense(
            @ToolParam(description = "员工ID") String employeeId,
            @ToolParam(description = "报销类型，如差旅报销、办公用品、招待费等") String expenseType,
            @ToolParam(description = "报销金额") double amount,
            @ToolParam(description = "报销事由") String reason) {
        try {
            log.info("提交报销单: employeeId={}, type={}, amount={}, reason={}", employeeId, expenseType, amount, reason);
            Map<String, Object> expenseData = new HashMap<>();
            expenseData.put("employeeId", employeeId);
            expenseData.put("expenseType", expenseType);
            expenseData.put("amount", amount);
            expenseData.put("reason", reason);

            Map<String, Object> result = fssPlatformClient.submitExpense(expenseData);
            return formatResult(result);
        } catch (Exception e) {
            log.error("提交报销单失败: employeeId={}", employeeId, e);
            return "报销单提交失败，请稍后重试或联系人工客服。";
        }
    }

    private String formatResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("报销单提交结果：\n");
        sb.append("- 报销单号: ").append(result.getOrDefault("expenseId", "未知")).append("\n");
        sb.append("- 状态: ").append(result.getOrDefault("status", "未知")).append("\n");
        sb.append("- 提交时间: ").append(result.getOrDefault("submittedAt", "未知")).append("\n");
        sb.append("- 提示: ").append(result.getOrDefault("message", ""));
        return sb.toString();
    }
}
