package com.company.finance.infrastructure.client;

import java.util.Map;

/**
 * ERP 系统客户端接口
 * <p>
 * 对接企业 ERP 系统，提供供应商信息查询等业务操作。
 * </p>
 */
public interface ERPClient {

    /**
     * 查询供应商信息
     *
     * @param supplierId 供应商 ID
     * @return 供应商基本信息（名称、联系方式、资质等）
     */
    Map<String, Object> querySupplierInfo(String supplierId);

    /**
     * 按名称搜索供应商
     *
     * @param supplierName 供应商名称（支持模糊匹配）
     * @return 匹配的供应商列表
     */
    Map<String, Object> searchSupplierByName(String supplierName);
}
