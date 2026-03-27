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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseSubmitToolTest {

    @Mock
    private FSSPlatformClient fssPlatformClient;

    private ExpenseSubmitTool tool;

    @BeforeEach
    void setUp() {
        tool = new ExpenseSubmitTool(fssPlatformClient);
    }

    @Test
    void submitExpense_success() {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("expenseId", "EXP-123");
        mockResult.put("status", "SUBMITTED");
        mockResult.put("message", "报销单提交成功，等待审批");
        when(fssPlatformClient.submitExpense(anyMap())).thenReturn(mockResult);

        String result = tool.submitExpense("EMP-001", "差旅", 3500.0, "北京出差");

        assertThat(result).contains("EXP-123");
        assertThat(result).contains("SUBMITTED");
        assertThat(result).contains("报销单提交成功");
    }

    @Test
    void submitExpense_externalSystemFailure_returnsErrorMessage() {
        when(fssPlatformClient.submitExpense(anyMap()))
                .thenThrow(new RuntimeException("Service unavailable"));

        String result = tool.submitExpense("EMP-001", "差旅", 3500.0, "北京出差");

        assertThat(result).contains("报销单提交失败");
        assertThat(result).doesNotContain("Service unavailable");
        assertThat(result).doesNotContain("RuntimeException");
    }
}
