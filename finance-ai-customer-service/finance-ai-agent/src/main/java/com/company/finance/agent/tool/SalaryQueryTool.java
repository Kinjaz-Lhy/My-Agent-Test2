package com.company.finance.agent.tool;

import com.company.finance.infrastructure.client.HRClient;
import kd.ai.nova.core.tool.annotation.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 薪资/个税/社保查询工具
 * <p>
 * 通过 HR 系统查询员工的薪资、个税、社保公积金信息。
 * </p>
 */
@Slf4j
@Component
public class SalaryQueryTool {

    private final HRClient hrClient;

    public SalaryQueryTool(HRClient hrClient) {
        this.hrClient = hrClient;
    }

    @Tool(description = "查询员工薪资信息，需要提供员工ID和年月（格式yyyy-MM），返回基本工资、奖金、津贴、实发工资等")
    public String querySalary(String employeeId, String yearMonth) {
        try {
            log.info("查询薪资信息: employeeId={}, yearMonth={}", employeeId, yearMonth);
            Map<String, Object> result = hrClient.querySalary(employeeId, yearMonth);
            return formatSalaryResult(result);
        } catch (Exception e) {
            log.error("查询薪资信息失败: employeeId={}, yearMonth={}", employeeId, yearMonth, e);
            return "查询薪资信息失败，请稍后重试或联系人工客服。";
        }
    }

    @Tool(description = "查询员工个税信息，需要提供员工ID和年月（格式yyyy-MM），返回应纳税所得额、税率、个税金额等")
    public String queryTax(String employeeId, String yearMonth) {
        try {
            log.info("查询个税信息: employeeId={}, yearMonth={}", employeeId, yearMonth);
            Map<String, Object> result = hrClient.queryTax(employeeId, yearMonth);
            return formatTaxResult(result);
        } catch (Exception e) {
            log.error("查询个税信息失败: employeeId={}, yearMonth={}", employeeId, yearMonth, e);
            return "查询个税信息失败，请稍后重试或联系人工客服。";
        }
    }

    @Tool(description = "查询员工社保公积金信息，需要提供员工ID和年月（格式yyyy-MM），返回养老保险、医疗保险、住房公积金等明细")
    public String querySocialInsurance(String employeeId, String yearMonth) {
        try {
            log.info("查询社保公积金信息: employeeId={}, yearMonth={}", employeeId, yearMonth);
            Map<String, Object> result = hrClient.querySocialInsurance(employeeId, yearMonth);
            return formatSocialInsuranceResult(result);
        } catch (Exception e) {
            log.error("查询社保公积金信息失败: employeeId={}, yearMonth={}", employeeId, yearMonth, e);
            return "查询社保公积金信息失败，请稍后重试或联系人工客服。";
        }
    }

    private String formatSalaryResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("薪资查询结果：\n");
        sb.append("- 年月: ").append(result.getOrDefault("yearMonth", "未知")).append("\n");
        sb.append("- 基本工资: ").append(result.getOrDefault("baseSalary", "未知")).append("\n");
        sb.append("- 奖金: ").append(result.getOrDefault("bonus", "未知")).append("\n");
        sb.append("- 津贴: ").append(result.getOrDefault("allowance", "未知")).append("\n");
        sb.append("- 应发合计: ").append(result.getOrDefault("totalSalary", "未知")).append("\n");
        sb.append("- 扣除合计: ").append(result.getOrDefault("deductions", "未知")).append("\n");
        sb.append("- 实发工资: ").append(result.getOrDefault("netSalary", "未知"));
        return sb.toString();
    }

    private String formatTaxResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("个税查询结果：\n");
        sb.append("- 年月: ").append(result.getOrDefault("yearMonth", "未知")).append("\n");
        sb.append("- 应纳税所得额: ").append(result.getOrDefault("taxableIncome", "未知")).append("\n");
        sb.append("- 税率: ").append(result.getOrDefault("taxRate", "未知")).append("\n");
        sb.append("- 速算扣除数: ").append(result.getOrDefault("quickDeduction", "未知")).append("\n");
        sb.append("- 当月个税: ").append(result.getOrDefault("taxAmount", "未知")).append("\n");
        sb.append("- 累计个税: ").append(result.getOrDefault("cumulativeTax", "未知"));
        return sb.toString();
    }

    private String formatSocialInsuranceResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("社保公积金查询结果：\n");
        sb.append("- 年月: ").append(result.getOrDefault("yearMonth", "未知")).append("\n");
        sb.append("- 养老保险: ").append(result.getOrDefault("pensionInsurance", "未知")).append("\n");
        sb.append("- 医疗保险: ").append(result.getOrDefault("medicalInsurance", "未知")).append("\n");
        sb.append("- 失业保险: ").append(result.getOrDefault("unemploymentInsurance", "未知")).append("\n");
        sb.append("- 住房公积金: ").append(result.getOrDefault("housingFund", "未知")).append("\n");
        sb.append("- 扣除合计: ").append(result.getOrDefault("totalDeduction", "未知"));
        return sb.toString();
    }
}
