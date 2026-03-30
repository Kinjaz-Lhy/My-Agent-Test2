package com.company.finance.agent.tool;

import com.company.finance.infrastructure.client.FSSPlatformClient;
import kd.ai.nova.core.tool.annotation.Tool;
import kd.ai.nova.core.tool.annotation.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 开票申请提交工具
 * <p>
 * 将员工填写的开票信息提交至财务共享平台。
 * </p>
 */
@Slf4j
@Component
public class BillingSubmitTool {

    private final FSSPlatformClient fssPlatformClient;

    public BillingSubmitTool(FSSPlatformClient fssPlatformClient) {
        this.fssPlatformClient = fssPlatformClient;
    }

    @Tool(description = "提交开票申请到财务共享平台，需要提供购买方名称、开票金额、发票类型、开票内容等信息，返回提交结果")
    public String submitBilling(
            @ToolParam(description = "购买方名称") String buyerName,
            @ToolParam(description = "开票金额") double amount,
            @ToolParam(description = "发票类型，如增值税专用发票、增值税普通发票") String invoiceType,
            @ToolParam(description = "开票内容") String content) {
        try {
            log.info("提交开票申请: buyerName={}, amount={}, invoiceType={}", buyerName, amount, invoiceType);
            Map<String, Object> billingData = new HashMap<>();
            billingData.put("buyerName", buyerName);
            billingData.put("amount", amount);
            billingData.put("invoiceType", invoiceType);
            billingData.put("content", content);

            Map<String, Object> result = fssPlatformClient.submitBilling(billingData);
            return formatResult(result);
        } catch (Exception e) {
            log.error("提交开票申请失败: buyerName={}", buyerName, e);
            return "开票申请提交失败，请稍后重试或联系人工客服。";
        }
    }

    private String formatResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("开票申请提交结果：\n");
        sb.append("- 开票申请号: ").append(result.getOrDefault("billingId", "未知")).append("\n");
        sb.append("- 状态: ").append(result.getOrDefault("status", "未知")).append("\n");
        sb.append("- 提交时间: ").append(result.getOrDefault("submittedAt", "未知")).append("\n");
        sb.append("- 提示: ").append(result.getOrDefault("message", ""));
        return sb.toString();
    }
}
