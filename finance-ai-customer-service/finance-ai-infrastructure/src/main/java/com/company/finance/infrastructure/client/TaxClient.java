package com.company.finance.infrastructure.client;

import java.util.Map;

/**
 * 税务验真接口客户端
 * <p>
 * 对接税务系统，提供发票验真等业务操作。
 * </p>
 */
public interface TaxClient {

    /**
     * 发票验真
     *
     * @param invoiceCode   发票代码
     * @param invoiceNumber 发票号码
     * @return 验真结果（包含真伪状态、发票详情等）
     */
    Map<String, Object> verifyInvoice(String invoiceCode, String invoiceNumber);
}
