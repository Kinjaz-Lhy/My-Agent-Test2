package com.company.finance.agent.tool;

import com.company.finance.infrastructure.client.ERPClient;
import kd.ai.nova.core.tool.annotation.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 供应商信息查询工具
 * <p>
 * 通过 ERP 系统查询供应商基本信息。
 * </p>
 */
@Slf4j
@Component
public class SupplierQueryTool {

    private final ERPClient erpClient;

    public SupplierQueryTool(ERPClient erpClient) {
        this.erpClient = erpClient;
    }

    @Tool(description = "根据供应商ID查询供应商基本信息，返回供应商名称、联系方式、资质状态等")
    public String querySupplierById(String supplierId) {
        try {
            log.info("查询供应商信息: supplierId={}", supplierId);
            Map<String, Object> result = erpClient.querySupplierInfo(supplierId);
            return formatSupplierInfo(result);
        } catch (Exception e) {
            log.error("查询供应商信息失败: supplierId={}", supplierId, e);
            return "查询供应商信息失败，请稍后重试或联系人工客服。";
        }
    }

    @Tool(description = "根据供应商名称模糊搜索供应商列表，返回匹配的供应商信息")
    public String searchSupplierByName(String supplierName) {
        try {
            log.info("搜索供应商: supplierName={}", supplierName);
            Map<String, Object> result = erpClient.searchSupplierByName(supplierName);
            return formatSearchResult(result);
        } catch (Exception e) {
            log.error("搜索供应商失败: supplierName={}", supplierName, e);
            return "搜索供应商失败，请稍后重试或联系人工客服。";
        }
    }

    private String formatSupplierInfo(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        sb.append("供应商信息：\n");
        sb.append("- 供应商ID: ").append(result.getOrDefault("supplierId", "未知")).append("\n");
        sb.append("- 名称: ").append(result.getOrDefault("supplierName", "未知")).append("\n");
        sb.append("- 联系人: ").append(result.getOrDefault("contactPerson", "未知")).append("\n");
        sb.append("- 联系电话: ").append(result.getOrDefault("contactPhone", "未知")).append("\n");
        sb.append("- 地址: ").append(result.getOrDefault("address", "未知")).append("\n");
        sb.append("- 资质: ").append(result.getOrDefault("qualification", "未知")).append("\n");
        sb.append("- 状态: ").append(result.getOrDefault("status", "未知"));
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String formatSearchResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();
        int total = ((Number) result.getOrDefault("total", 0)).intValue();
        sb.append("供应商搜索结果（共 ").append(total).append(" 条）：\n");

        Object suppliersObj = result.get("suppliers");
        if (suppliersObj instanceof List) {
            List<Map<String, Object>> suppliers = (List<Map<String, Object>>) suppliersObj;
            for (Map<String, Object> supplier : suppliers) {
                sb.append("- ").append(supplier.getOrDefault("supplierId", ""))
                  .append(" | ").append(supplier.getOrDefault("supplierName", ""))
                  .append(" | 状态: ").append(supplier.getOrDefault("status", "未知"))
                  .append("\n");
            }
        }
        return sb.toString();
    }
}
