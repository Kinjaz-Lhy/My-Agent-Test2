package com.company.finance.common.constants;

import java.util.regex.Pattern;

/**
 * 敏感信息正则模式常量
 * <p>
 * 定义需要脱敏处理的敏感信息匹配模式，
 * 包括身份证号、银行卡号、工资金额等。
 * 用于 DataMaskingAdvisor 在 AI 响应中执行脱敏替换。
 * </p>
 */
public final class SensitivePatterns {

    private SensitivePatterns() {
        // 工具类禁止实例化
    }

    /**
     * 身份证号正则（18 位，最后一位可为 X/x）
     * 示例：110101199003071234、11010119900307123X
     */
    public static final String ID_CARD_REGEX = "\\d{17}[\\dXx]";
    public static final Pattern ID_CARD_PATTERN = Pattern.compile(ID_CARD_REGEX);

    /**
     * 银行卡号正则（16-19 位数字）
     * 示例：6222021234567890123
     */
    public static final String BANK_CARD_REGEX = "\\d{16,19}";
    public static final Pattern BANK_CARD_PATTERN = Pattern.compile(BANK_CARD_REGEX);

    /**
     * 工资金额正则（匹配"工资/薪资/月薪/年薪"后跟的金额数字）
     * 示例：工资12000.50元、薪资 8000、月薪15000
     */
    public static final String SALARY_AMOUNT_REGEX = "(?<=(?:工资|薪资|月薪|年薪|税后|税前|实发|应发)\\s{0,2})\\d+(?:\\.\\d{1,2})?";
    public static final Pattern SALARY_AMOUNT_PATTERN = Pattern.compile(SALARY_AMOUNT_REGEX);

    /** 身份证号脱敏替换文本 */
    public static final String ID_CARD_MASK = "***************";

    /** 银行卡号脱敏替换文本 */
    public static final String BANK_CARD_MASK = "****";

    /** 工资金额脱敏替换文本 */
    public static final String SALARY_MASK = "***";
}
