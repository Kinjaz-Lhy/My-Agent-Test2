package com.company.finance.service.advisor;

import com.company.finance.common.constants.SensitivePatterns;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataMaskingService 单元测试
 * <p>
 * 验证身份证号、银行卡号、工资金额的脱敏处理逻辑。
 * </p>
 */
class DataMaskingServiceTest {

    private DataMaskingService service;

    @BeforeEach
    void setUp() {
        service = new DataMaskingService();
    }

    // ========== 空值与边界 ==========

    @Test
    @DisplayName("null 输入返回 null")
    void shouldReturnNullForNullInput() {
        assertThat(service.mask(null)).isNull();
    }

    @Test
    @DisplayName("空字符串返回空字符串")
    void shouldReturnEmptyForEmptyInput() {
        assertThat(service.mask("")).isEmpty();
    }

    @Test
    @DisplayName("不含敏感信息的文本原样返回")
    void shouldReturnOriginalWhenNoSensitiveData() {
        String input = "您好，请问有什么可以帮您？";
        assertThat(service.mask(input)).isEqualTo(input);
    }

    // ========== 身份证号脱敏 ==========

    @Test
    @DisplayName("脱敏18位身份证号（数字结尾）")
    void shouldMaskIdCardWithDigitEnding() {
        String input = "您的身份证号是110101199003071234";
        String masked = service.mask(input);
        assertThat(masked).contains(SensitivePatterns.ID_CARD_MASK);
        assertThat(masked).doesNotContain("110101199003071234");
    }

    @Test
    @DisplayName("脱敏18位身份证号（X结尾）")
    void shouldMaskIdCardWithXEnding() {
        String input = "身份证号：11010119900307123X";
        String masked = service.mask(input);
        assertThat(masked).contains(SensitivePatterns.ID_CARD_MASK);
        assertThat(masked).doesNotContain("11010119900307123X");
    }

    @Test
    @DisplayName("脱敏18位身份证号（小写x结尾）")
    void shouldMaskIdCardWithLowerXEnding() {
        String input = "身份证：11010119900307123x";
        String masked = service.mask(input);
        assertThat(masked).contains(SensitivePatterns.ID_CARD_MASK);
        assertThat(masked).doesNotContain("11010119900307123x");
    }

    // ========== 银行卡号脱敏 ==========

    @Test
    @DisplayName("脱敏19位银行卡号")
    void shouldMask19DigitBankCard() {
        String input = "银行卡号：6222021234567890123";
        String masked = service.mask(input);
        assertThat(masked).contains(SensitivePatterns.BANK_CARD_MASK);
        assertThat(masked).doesNotContain("6222021234567890123");
    }

    @Test
    @DisplayName("脱敏16位银行卡号")
    void shouldMask16DigitBankCard() {
        String input = "卡号6222021234567890";
        String masked = service.mask(input);
        assertThat(masked).contains(SensitivePatterns.BANK_CARD_MASK);
        assertThat(masked).doesNotContain("6222021234567890");
    }

    // ========== 工资金额脱敏 ==========

    @Test
    @DisplayName("脱敏'工资'后的金额")
    void shouldMaskSalaryAmount() {
        String input = "您的工资12000元已发放";
        String masked = service.mask(input);
        assertThat(masked).contains("工资" + SensitivePatterns.SALARY_MASK);
        assertThat(masked).doesNotContain("12000");
    }

    @Test
    @DisplayName("脱敏'薪资'后的金额")
    void shouldMaskSalaryWithXinZi() {
        String input = "薪资8000.50元";
        String masked = service.mask(input);
        assertThat(masked).contains("薪资" + SensitivePatterns.SALARY_MASK);
        assertThat(masked).doesNotContain("8000.50");
    }

    @Test
    @DisplayName("脱敏'月薪'后的金额")
    void shouldMaskMonthlySalary() {
        String input = "月薪15000";
        String masked = service.mask(input);
        assertThat(masked).contains("月薪" + SensitivePatterns.SALARY_MASK);
        assertThat(masked).doesNotContain("15000");
    }

    @Test
    @DisplayName("脱敏'年薪'后的金额")
    void shouldMaskAnnualSalary() {
        String input = "年薪200000元";
        String masked = service.mask(input);
        assertThat(masked).contains("年薪" + SensitivePatterns.SALARY_MASK);
        assertThat(masked).doesNotContain("200000");
    }

    @Test
    @DisplayName("脱敏'税后'后的金额")
    void shouldMaskAfterTaxAmount() {
        String input = "税后9500元";
        String masked = service.mask(input);
        assertThat(masked).contains("税后" + SensitivePatterns.SALARY_MASK);
        assertThat(masked).doesNotContain("9500");
    }

    // ========== 混合场景 ==========

    @Test
    @DisplayName("同时包含身份证号和工资金额的文本")
    void shouldMaskMultipleSensitiveTypes() {
        String input = "员工身份证110101199003071234，工资12000元";
        String masked = service.mask(input);
        assertThat(masked).doesNotContain("110101199003071234");
        assertThat(masked).doesNotContain("12000");
        assertThat(masked).contains(SensitivePatterns.ID_CARD_MASK);
    }

    @Test
    @DisplayName("脱敏后的文本不再匹配身份证号正则")
    void maskedTextShouldNotMatchIdCardPattern() {
        String input = "身份证号：110101199003071234";
        String masked = service.mask(input);
        assertThat(SensitivePatterns.ID_CARD_PATTERN.matcher(masked).find()).isFalse();
    }

    @Test
    @DisplayName("脱敏后的文本不再匹配工资金额正则")
    void maskedTextShouldNotMatchSalaryPattern() {
        String input = "工资12000.50元";
        String masked = service.mask(input);
        assertThat(SensitivePatterns.SALARY_AMOUNT_PATTERN.matcher(masked).find()).isFalse();
    }
}
