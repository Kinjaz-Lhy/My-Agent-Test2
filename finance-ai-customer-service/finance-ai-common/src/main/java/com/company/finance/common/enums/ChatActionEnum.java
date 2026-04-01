package com.company.finance.common.enums;

/**
 * 对话操作类型枚举，用于审计日志的 action 字段。
 * 与前端意图分类下拉选项一一对应。
 */
public enum ChatActionEnum {

    CHAT("CHAT", "闲聊"),
    EXPENSE_QUERY("EXPENSE_QUERY", "报销查询"),
    INVOICE_VERIFY("INVOICE_VERIFY", "发票验真"),
    SALARY_QUERY("SALARY_QUERY", "薪资查询"),
    SUPPLIER_QUERY("SUPPLIER_QUERY", "供应商查询"),
    GUIDE("GUIDE", "流程引导"),
    HANDOFF("HANDOFF", "人工转接");

    private final String code;
    private final String label;

    ChatActionEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
