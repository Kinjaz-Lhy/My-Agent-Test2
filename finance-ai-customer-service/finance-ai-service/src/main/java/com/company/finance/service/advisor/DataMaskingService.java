package com.company.finance.service.advisor;

import com.company.finance.common.constants.SensitivePatterns;

/**
 * 敏感信息脱敏服务
 * <p>
 * 对文本中的身份证号、银行卡号、工资金额等敏感信息执行正则匹配与掩码替换。
 * 独立于 AI-Nova Advisor 框架，方便单元测试和复用。
 * </p>
 *
 * @see SensitivePatterns
 */
public class DataMaskingService {

    /**
     * 对输入文本执行敏感信息脱敏处理
     * <p>
     * 按顺序依次替换：身份证号 → 银行卡号 → 工资金额。
     * 身份证号优先于银行卡号匹配，避免 18 位身份证号被银行卡号正则误匹配。
     * </p>
     *
     * @param input 原始文本，可能包含敏感信息
     * @return 脱敏后的文本；如果输入为 null 或空字符串，原样返回
     */
    public String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;

        // 1. 先替换身份证号（18位，末位可为X/x）
        //    身份证号长度固定18位，优先匹配可避免被银行卡号正则（16-19位）误匹配
        result = SensitivePatterns.ID_CARD_PATTERN.matcher(result).replaceAll(SensitivePatterns.ID_CARD_MASK);

        // 2. 替换银行卡号（16-19位数字）
        //    此时身份证号已被替换为掩码字符，不会被银行卡号正则重复匹配
        result = SensitivePatterns.BANK_CARD_PATTERN.matcher(result).replaceAll(SensitivePatterns.BANK_CARD_MASK);

        // 3. 替换工资金额（"工资/薪资/月薪/年薪"等关键词后跟的数字）
        result = SensitivePatterns.SALARY_AMOUNT_PATTERN.matcher(result).replaceAll(SensitivePatterns.SALARY_MASK);

        return result;
    }
}
