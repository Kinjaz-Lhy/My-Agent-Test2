package com.company.finance.infrastructure.client.mock;

import com.company.finance.infrastructure.client.ERPClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 系统客户端 Mock 实现
 * <p>
 * 用于开发和测试环境，返回模拟数据，不调用真实外部系统。
 * </p>
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class MockERPClient implements ERPClient {

    @Override
    public Map<String, Object> querySupplierInfo(String supplierId) {
        log.info("[Mock-ERP] 查询供应商信息: supplierId={}", supplierId);
        Map<String, Object> result = new HashMap<>();
        result.put("supplierId", supplierId);
        result.put("supplierName", "模拟供应商有限公司");
        result.put("contactPerson", "张三");
        result.put("contactPhone", "010-12345678");
        result.put("address", "北京市海淀区中关村大街1号");
        result.put("qualification", "一般纳税人");
        result.put("status", "ACTIVE");
        return result;
    }

    @Override
    public Map<String, Object> searchSupplierByName(String supplierName) {
        log.info("[Mock-ERP] 按名称搜索供应商: supplierName={}", supplierName);
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> suppliers = new ArrayList<>();

        Map<String, Object> supplier = new HashMap<>();
        supplier.put("supplierId", "SUP-001");
        supplier.put("supplierName", supplierName + "（模拟匹配）");
        supplier.put("status", "ACTIVE");
        suppliers.add(supplier);

        result.put("total", 1);
        result.put("suppliers", suppliers);
        return result;
    }
}
