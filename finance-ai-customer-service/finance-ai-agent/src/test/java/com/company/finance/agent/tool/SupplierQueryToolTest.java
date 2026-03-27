package com.company.finance.agent.tool;

import com.company.finance.infrastructure.client.ERPClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierQueryToolTest {

    @Mock
    private ERPClient erpClient;

    private SupplierQueryTool tool;

    @BeforeEach
    void setUp() {
        tool = new SupplierQueryTool(erpClient);
    }

    @Test
    void querySupplierById_success() {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("supplierId", "SUP-001");
        mockResult.put("supplierName", "测试供应商");
        mockResult.put("status", "ACTIVE");
        when(erpClient.querySupplierInfo("SUP-001")).thenReturn(mockResult);

        String result = tool.querySupplierById("SUP-001");

        assertThat(result).contains("SUP-001");
        assertThat(result).contains("测试供应商");
        assertThat(result).contains("ACTIVE");
    }

    @Test
    void querySupplierById_externalSystemFailure_returnsErrorMessage() {
        when(erpClient.querySupplierInfo(anyString()))
                .thenThrow(new RuntimeException("ERP unavailable"));

        String result = tool.querySupplierById("SUP-001");

        assertThat(result).contains("查询供应商信息失败");
        assertThat(result).doesNotContain("ERP unavailable");
    }

    @Test
    void searchSupplierByName_success() {
        Map<String, Object> mockResult = new HashMap<>();
        List<Map<String, Object>> suppliers = new ArrayList<>();
        Map<String, Object> supplier = new HashMap<>();
        supplier.put("supplierId", "SUP-001");
        supplier.put("supplierName", "测试供应商");
        supplier.put("status", "ACTIVE");
        suppliers.add(supplier);
        mockResult.put("total", 1);
        mockResult.put("suppliers", suppliers);
        when(erpClient.searchSupplierByName("测试")).thenReturn(mockResult);

        String result = tool.searchSupplierByName("测试");

        assertThat(result).contains("共 1 条");
        assertThat(result).contains("SUP-001");
    }

    @Test
    void searchSupplierByName_externalSystemFailure_returnsErrorMessage() {
        when(erpClient.searchSupplierByName(anyString()))
                .thenThrow(new RuntimeException("ERP timeout"));

        String result = tool.searchSupplierByName("测试");

        assertThat(result).contains("搜索供应商失败");
        assertThat(result).doesNotContain("ERP timeout");
    }
}
