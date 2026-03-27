package com.company.finance.agent.tool;

import com.company.finance.infrastructure.client.FSSPlatformClient;
import kd.ai.nova.core.tool.annotation.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 借款单/付款申请查询工具
 * <p>
 * 通过财务共享平台查询借款单或付款申请的状态。
 * </p>
 */
@Slf4j
@Component
public class LoanQueryTool {

    private final FSSPlatformClient fssPlatformClient;

    public LoanQueryTool(FSSPlatformClient fssPlatformClient) {
        this.fssPlatformClient = fssPlatformClient;
    }

    @Tool(description = "根据借款单号和员工ID查询借款单或付款申请状态，返回单据当前状态、金额、用途等信息")
    public String queryLoanStatus(String loanId, String employeeId) {
        try {
            log.info("查询借款单状态: loanId={}, employeeId={}", loanId, employeeId);
            Map<String, Object> result = fssPlatformClient.queryLoanStatus(loanId, employeeId);
            return formatResult(result);
        } catch (Exception e) {
            log.error("查询借款单状态失败: loanId={}, employeeId={}", loanId, employeeId, e);
            return "查询借款单状态失败，请稍后重试或联系人工客服。";
        }
    }

    private String formatResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("借款单查询结果：\n");
        sb.append("- 借款单号: ").append(result.getOrDefault("loanId", "未知")).append("\n");
        sb.append("- 状态: ").append(result.getOrDefault("status", "未知")).append("\n");
        sb.append("- 金额: ").append(result.getOrDefault("amount", "未知")).append("\n");
        sb.append("- 用途: ").append(result.getOrDefault("purpose", "未知")).append("\n");
        sb.append("- 提交时间: ").append(result.getOrDefault("submittedAt", "未知"));
        return sb.toString();
    }
}
