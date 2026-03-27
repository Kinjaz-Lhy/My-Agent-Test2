package com.company.finance.agent.tool;

import com.company.finance.infrastructure.client.TaxClient;
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
class InvoiceVerifyToolTest {

    @Mock
    private TaxClient taxClient;

    private InvoiceVerifyTool tool;

    @BeforeEach
    void setUp() {
        tool = new InvoiceVerifyTool(taxClient);
    }

    @Test
    void verifyInvoice_valid() {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("invoiceCode", "1100182130");
        mockResult.put("invoiceNumber", "04177154");
        mockResult.put("valid", true);
        mockResult.put("invoiceType", "增值税专用发票");
        mockResult.put("amount", 5000.00);
        when(taxClient.verifyInvoice("1100182130", "04177154")).thenReturn(mockResult);

        String result = tool.verifyInvoice("1100182130", "04177154");

        assertThat(result).contains("1100182130");
        assertThat(result).contains("真票");
        assertThat(result).contains("增值税专用发票");
    }

    @Test
    void verifyInvoice_invalid() {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("valid", false);
        when(taxClient.verifyInvoice("FAKE", "0000")).thenReturn(mockResult);

        String result = tool.verifyInvoice("FAKE", "0000");

        assertThat(result).contains("存疑");
    }

    @Test
    void verifyInvoice_externalSystemFailure_returnsErrorMessage() {
        when(taxClient.verifyInvoice(anyString(), anyString()))
                .thenThrow(new RuntimeException("Tax service down"));

        String result = tool.verifyInvoice("1100182130", "04177154");

        assertThat(result).contains("发票验真失败");
        assertThat(result).doesNotContain("Tax service down");
        assertThat(result).doesNotContain("RuntimeException");
    }
}
