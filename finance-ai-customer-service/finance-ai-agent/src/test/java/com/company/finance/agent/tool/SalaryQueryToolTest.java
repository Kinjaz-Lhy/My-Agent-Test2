package com.company.finance.agent.tool;

import com.company.finance.infrastructure.client.HRClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryQueryToolTest {

    @Mock
    private HRClient hrClient;

    private SalaryQueryTool tool;

    @BeforeEach
    void setUp() {
        tool = new SalaryQueryTool(hrClient);
    }

    @Test
    void querySalary_success() {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("yearMonth", "2024-01");
        mockResult.put("baseSalary", 15000.00);
        mockResult.put("netSalary", 12000.00);
        when(hrClient.querySalary("EMP-001", "2024-01")).thenReturn(mockResult);

        String result = tool.querySalary("EMP-001", "2024-01");

        assertThat(result).contains("2024-01");
        assertThat(result).contains("15000.0");
        assertThat(result).contains("12000.0");
    }

    @Test
    void querySalary_externalSystemFailure_returnsErrorMessage() {
        when(hrClient.querySalary(anyString(), anyString()))
                .thenThrow(new RuntimeException("HR system error"));

        String result = tool.querySalary("EMP-001", "2024-01");

        assertThat(result).contains("查询薪资信息失败");
        assertThat(result).doesNotContain("HR system error");
    }

    @Test
    void queryTax_success() {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("yearMonth", "2024-01");
        mockResult.put("taxAmount", 1290.00);
        when(hrClient.queryTax("EMP-001", "2024-01")).thenReturn(mockResult);

        String result = tool.queryTax("EMP-001", "2024-01");

        assertThat(result).contains("1290.0");
    }

    @Test
    void queryTax_externalSystemFailure_returnsErrorMessage() {
        when(hrClient.queryTax(anyString(), anyString()))
                .thenThrow(new RuntimeException("Tax query failed"));

        String result = tool.queryTax("EMP-001", "2024-01");

        assertThat(result).contains("查询个税信息失败");
        assertThat(result).doesNotContain("Tax query failed");
    }

    @Test
    void querySocialInsurance_success() {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("yearMonth", "2024-01");
        mockResult.put("housingFund", 1800.00);
        mockResult.put("totalDeduction", 3500.00);
        when(hrClient.querySocialInsurance("EMP-001", "2024-01")).thenReturn(mockResult);

        String result = tool.querySocialInsurance("EMP-001", "2024-01");

        assertThat(result).contains("1800.0");
        assertThat(result).contains("3500.0");
    }

    @Test
    void querySocialInsurance_externalSystemFailure_returnsErrorMessage() {
        when(hrClient.querySocialInsurance(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection refused"));

        String result = tool.querySocialInsurance("EMP-001", "2024-01");

        assertThat(result).contains("查询社保公积金信息失败");
        assertThat(result).doesNotContain("Connection refused");
    }
}
