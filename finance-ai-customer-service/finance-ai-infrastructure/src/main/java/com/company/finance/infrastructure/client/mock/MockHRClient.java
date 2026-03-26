package com.company.finance.infrastructure.client.mock;

import com.company.finance.infrastructure.client.HRClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * HR 系统客户端 Mock 实现
 * <p>
 * 用于开发和测试环境，返回模拟数据，不调用真实外部系统。
 * </p>
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class MockHRClient implements HRClient {

    @Override
    public Map<String, Object> querySalary(String employeeId, String yearMonth) {
        log.info("[Mock-HR] 查询薪资信息: employeeId={}, yearMonth={}", employeeId, yearMonth);
        Map<String, Object> result = new HashMap<>();
        result.put("employeeId", employeeId);
        result.put("yearMonth", yearMonth);
        result.put("baseSalary", 15000.00);
        result.put("bonus", 3000.00);
        result.put("allowance", 2000.00);
        result.put("totalSalary", 20000.00);
        result.put("deductions", 4500.00);
        result.put("netSalary", 15500.00);
        return result;
    }

    @Override
    public Map<String, Object> queryTax(String employeeId, String yearMonth) {
        log.info("[Mock-HR] 查询个税信息: employeeId={}, yearMonth={}", employeeId, yearMonth);
        Map<String, Object> result = new HashMap<>();
        result.put("employeeId", employeeId);
        result.put("yearMonth", yearMonth);
        result.put("taxableIncome", 15000.00);
        result.put("taxRate", 0.10);
        result.put("quickDeduction", 210.00);
        result.put("taxAmount", 1290.00);
        result.put("cumulativeTax", 7740.00);
        return result;
    }

    @Override
    public Map<String, Object> querySocialInsurance(String employeeId, String yearMonth) {
        log.info("[Mock-HR] 查询社保公积金信息: employeeId={}, yearMonth={}", employeeId, yearMonth);
        Map<String, Object> result = new HashMap<>();
        result.put("employeeId", employeeId);
        result.put("yearMonth", yearMonth);
        result.put("pensionInsurance", 1200.00);
        result.put("medicalInsurance", 400.00);
        result.put("unemploymentInsurance", 100.00);
        result.put("housingFund", 1800.00);
        result.put("totalDeduction", 3500.00);
        return result;
    }
}
