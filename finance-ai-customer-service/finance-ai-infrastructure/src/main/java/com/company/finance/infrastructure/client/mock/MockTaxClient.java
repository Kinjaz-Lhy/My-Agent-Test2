package com.company.finance.infrastructure.client.mock;

import com.company.finance.infrastructure.client.TaxClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 税务验真接口客户端 Mock 实现
 * <p>
 * 用于开发和测试环境，返回模拟数据，不调用真实外部系统。
 * </p>
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class MockTaxClient implements TaxClient {

    @Override
    public Map<String, Object> verifyInvoice(String invoiceCode, String invoiceNumber) {
        log.info("[Mock-Tax] 发票验真: invoiceCode={}, invoiceNumber={}", invoiceCode, invoiceNumber);
        Map<String, Object> result = new HashMap<>();
        result.put("invoiceCode", invoiceCode);
        result.put("invoiceNumber", invoiceNumber);
        result.put("valid", true);
        result.put("invoiceType", "增值税专用发票");
        result.put("amount", 5000.00);
        result.put("taxAmount", 650.00);
        result.put("sellerName", "模拟销售方有限公司");
        result.put("buyerName", "本公司");
        result.put("invoiceDate", "2024-01-15");
        result.put("verifyMessage", "发票验真通过");
        return result;
    }
}
