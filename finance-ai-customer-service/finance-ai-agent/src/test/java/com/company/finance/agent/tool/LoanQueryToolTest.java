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
class LoanQueryToolTest {

    @Mock
    private FSSPlatformClient fssPlatformClient;

    private LoanQueryTool tool;

    @BeforeEach
    void setUp() {
        tool = new LoanQueryTool(fssPlatformClient);
    }

    @Test
    void queryLoanStatus_success() {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("loanId", "LOAN-001");
        mockResult.put("status", "PENDING_APPROVAL");
        mockResult.put("amount", 10000.00);
        mockResult.put("purpose", "项目预付款");
        when(fssPlatformClient.queryLoanStatus("LOAN-001", "EMP-001")).thenReturn(mockResult);

        String result = tool.queryLoanStatus("LOAN-001", "EMP-001");

        assertThat(result).contains("LOAN-001");
        assertThat(result).contains("PENDING_APPROVAL");
        assertThat(result).contains("10000.0");
        assertThat(result).contains("项目预付款");
    }

    @Test
    void queryLoanStatus_externalSystemFailure_returnsErrorMessage() {
        when(fssPlatformClient.queryLoanStatus(anyString(), anyString()))
                .thenThrow(new RuntimeException("Network error"));

        String result = tool.queryLoanStatus("LOAN-001", "EMP-001");

        assertThat(result).contains("查询借款单状态失败");
        assertThat(result).doesNotContain("Network error");
        assertThat(result).doesNotContain("RuntimeException");
    }
}
