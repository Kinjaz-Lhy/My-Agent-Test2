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
class BillingSubmitToolTest {

    @Mock
    private FSSPlatformClient fssPlatformClient;

    private BillingSubmitTool tool;

    @BeforeEach
    void setUp() {
        tool = new BillingSubmitTool(fssPlatformClient);
    }

    @Test
    void submitBilling_success() {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("billingId", "BIL-001");
        mockResult.put("status", "SUBMITTED");
        mockResult.put("message", "开票申请提交成功");
        when(fssPlatformClient.submitBilling(anyMap())).thenReturn(mockResult);

        String result = tool.submitBilling("购买方公司", 10000.0, "增值税专用发票", "技术服务费");

        assertThat(result).contains("BIL-001");
        assertThat(result).contains("SUBMITTED");
        assertThat(result).contains("开票申请提交成功");
    }

    @Test
    void submitBilling_externalSystemFailure_returnsErrorMessage() {
        when(fssPlatformClient.submitBilling(anyMap()))
                .thenThrow(new RuntimeException("FSS platform error"));

        String result = tool.submitBilling("购买方公司", 10000.0, "增值税专用发票", "技术服务费");

        assertThat(result).contains("开票申请提交失败");
        assertThat(result).doesNotContain("FSS platform error");
        assertThat(result).doesNotContain("RuntimeException");
    }
}
