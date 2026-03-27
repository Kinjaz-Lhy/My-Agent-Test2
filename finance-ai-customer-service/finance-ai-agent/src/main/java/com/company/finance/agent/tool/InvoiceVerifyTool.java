package com.company.finance.agent.tool;

import com.company.finance.infrastructure.client.TaxClient;
import kd.ai.nova.core.tool.annotation.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 发票验真工具
 * <p>
 * 调用税务验真接口验证发票真伪。
 * </p>
 */
@Slf4j
@Component
public class InvoiceVerifyTool {

    private final TaxClient taxClient;

    public InvoiceVerifyTool(TaxClient taxClient) {
        this.taxClient = taxClient;
    }

    @Tool(description = "调用税务接口验证发票真伪，需要提供发票代码和发票号码，返回验真结果")
    public String verifyInvoice(String invoiceCode, String invoiceNumber) {
        try {
            log.info("发票验真: invoiceCode={}, invoiceNumber={}", invoiceCode, invoiceNumber);
            Map<String, Object> result = taxClient.verifyInvoice(invoiceCode, invoiceNumber);
            return formatResult(result);
        } catch (Exception e) {
            log.error("发票验真失败: invoiceCode={}, invoiceNumber={}", invoiceCode, invoiceNumber, e);
            return "发票验真失败，请稍后重试或联系人工客服。";
        }
    }

    private String formatResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("发票验真结果：\n");
        sb.append("- 发票代码: ").append(result.getOrDefault("invoiceCode", "未知")).append("\n");
        sb.append("- 发票号码: ").append(result.getOrDefault("invoiceNumber", "未知")).append("\n");
        sb.append("- 验证结果: ").append(Boolean.TRUE.equals(result.get("valid")) ? "真票" : "存疑").append("\n");
        sb.append("- 发票类型: ").append(result.getOrDefault("invoiceType", "未知")).append("\n");
        sb.append("- 金额: ").append(result.getOrDefault("amount", "未知")).append("\n");
        sb.append("- 税额: ").append(result.getOrDefault("taxAmount", "未知")).append("\n");
        sb.append("- 销售方: ").append(result.getOrDefault("sellerName", "未知")).append("\n");
        sb.append("- 开票日期: ").append(result.getOrDefault("invoiceDate", "未知")).append("\n");
        sb.append("- 验证说明: ").append(result.getOrDefault("verifyMessage", ""));
        return sb.toString();
    }
}
