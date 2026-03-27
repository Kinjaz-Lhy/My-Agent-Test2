package com.company.finance.agent.tool;

import com.company.finance.infrastructure.client.FSSPlatformClient;
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
class ExpenseQueryToolTest {

    @Mock
    private FSSPlatformClient fssPlatformClient;

    private ExpenseQueryTool tool;

    @BeforeEach
    void setUp() {
        tool = new ExpenseQueryTool(fssPlatformClient);
    }

    @Test
    void queryExpenseStatus_success() {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("expenseId", "EXP-001");
        mockResult.put("status", "APPROVED");
        mockResult.put("amount", 3500.00);
        mockResult.put("currentStep", "财务审核");
        when(fssPlatformClient.queryExpenseStatus("EXP-001", "EMP-001")).thenReturn(mockResult);

        String result = tool.queryExpenseStatus("EXP-001", "EMP-001");

        assertThat(result).contains("EXP-001");
        assertThat(result).contains("APPROVED");
        assertThat(result).contains("3500.0");
    }

    @Test
    void queryExpenseStatus_externalSystemFailure_returnsErrorMessage() {
        when(fssPlatformClient.queryExpenseStatus(anyString(), anyString()))
                .thenThrow(new RuntimeException("Connection timeout"));

        String result = tool.queryExpenseStatus("EXP-001", "EMP-001");

        assertThat(result).contains("查询报销单状态失败");
        assertThat(result).doesNotContain("Connection timeout");
        assertThat(result).doesNotContain("RuntimeException");
    }
}
