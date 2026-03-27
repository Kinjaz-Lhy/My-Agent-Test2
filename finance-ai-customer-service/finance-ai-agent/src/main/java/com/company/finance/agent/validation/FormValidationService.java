package com.company.finance.agent.validation;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 表单验证服务
 * <p>
 * 验证单据必填字段完整性和字段值规范性，
 * 返回所有错误字段名称和描述列表。
 * </p>
 *
 * @see <a href="需求 4.2, 4.3">流程引导与异常处理</a>
 */
@Service
public class FormValidationService {

    /** 员工ID格式：字母开头 + 数字 */
    static final Pattern EMPLOYEE_ID_PATTERN = Pattern.compile("^[A-Za-z]+\\d+$");

    /** 金额格式：正数，最多两位小数 */
    static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");

    /** 日期格式 */
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 验证报销单表单
     *
     * @param formData 表单数据
     * @return 验证错误列表，空列表表示验证通过
     */
    public List<ValidationError> validateExpenseForm(Map<String, String> formData) {
        List<ValidationError> errors = new ArrayList<>();

        // 必填字段检查
        requireField(formData, "employeeId", "员工ID", errors);
        requireField(formData, "expenseType", "报销类型", errors);
        requireField(formData, "amount", "报销金额", errors);
        requireField(formData, "reason", "报销事由", errors);
        requireField(formData, "date", "发生日期", errors);

        // 字段值规范性检查
        validateEmployeeId(formData.get("employeeId"), errors);
        validateAmount(formData.get("amount"), "报销金额", errors);
        validateDate(formData.get("date"), "发生日期", errors);
        validateExpenseType(formData.get("expenseType"), errors);

        return errors;
    }

    /**
     * 验证发票表单
     *
     * @param formData 表单数据
     * @return 验证错误列表
     */
    public List<ValidationError> validateInvoiceForm(Map<String, String> formData) {
        List<ValidationError> errors = new ArrayList<>();

        requireField(formData, "invoiceCode", "发票代码", errors);
        requireField(formData, "invoiceNumber", "发票号码", errors);

        String invoiceCode = formData.get("invoiceCode");
        if (invoiceCode != null && !invoiceCode.isEmpty() && !invoiceCode.matches("^\\d{10,12}$")) {
            errors.add(new ValidationError("invoiceCode", "发票代码",
                    "发票代码应为10-12位数字", "例如：1100192130"));
        }

        String invoiceNumber = formData.get("invoiceNumber");
        if (invoiceNumber != null && !invoiceNumber.isEmpty() && !invoiceNumber.matches("^\\d{8}$")) {
            errors.add(new ValidationError("invoiceNumber", "发票号码",
                    "发票号码应为8位数字", "例如：04177562"));
        }

        return errors;
    }

    /**
     * 通用表单验证：检查必填字段和基本格式
     *
     * @param formData       表单数据
     * @param requiredFields 必填字段名称映射（字段key → 显示名称）
     * @return 验证错误列表
     */
    public List<ValidationError> validateRequiredFields(Map<String, String> formData,
                                                         Map<String, String> requiredFields) {
        List<ValidationError> errors = new ArrayList<>();
        for (Map.Entry<String, String> entry : requiredFields.entrySet()) {
            requireField(formData, entry.getKey(), entry.getValue(), errors);
        }
        return errors;
    }

    /**
     * 分析退回原因并生成修改建议
     *
     * @param rejectionReason 退回原因描述
     * @return 修改建议列表
     */
    public List<String> analyzeRejectionReason(String rejectionReason) {
        List<String> suggestions = new ArrayList<>();

        if (rejectionReason == null || rejectionReason.isEmpty()) {
            suggestions.add("请联系审批人了解具体退回原因");
            return suggestions;
        }

        String reason = rejectionReason.toLowerCase();

        if (reason.contains("金额") || reason.contains("超标") || reason.contains("超出标准")) {
            suggestions.add("请核实报销金额是否符合公司报销标准，必要时调整金额或补充超标审批说明");
        }
        if (reason.contains("发票") || reason.contains("票据")) {
            suggestions.add("请检查发票是否齐全、清晰，确保发票金额与报销金额一致");
        }
        if (reason.contains("材料") || reason.contains("附件") || reason.contains("缺少")) {
            suggestions.add("请补充缺失的证明材料或附件，确保所有必要文件已上传");
        }
        if (reason.contains("审批") || reason.contains("流程")) {
            suggestions.add("请确认审批流程是否正确，必要时重新选择审批人");
        }
        if (reason.contains("日期") || reason.contains("时间") || reason.contains("过期")) {
            suggestions.add("请核实日期信息是否正确，确保在有效报销期限内");
        }
        if (reason.contains("信息") || reason.contains("填写") || reason.contains("错误")) {
            suggestions.add("请仔细核对填写的信息，修正错误字段后重新提交");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("退回原因：" + rejectionReason);
            suggestions.add("建议联系审批人了解详细退回原因后进行修改");
        }

        return suggestions;
    }

    /**
     * 检查缺失材料并返回材料清单
     *
     * @param submittedMaterials 已提交的材料列表
     * @param requiredMaterials  必需的材料列表
     * @return 缺失材料列表
     */
    public List<String> checkMissingMaterials(List<String> submittedMaterials,
                                               List<String> requiredMaterials) {
        List<String> missing = new ArrayList<>();
        for (String required : requiredMaterials) {
            boolean found = submittedMaterials.stream()
                    .anyMatch(s -> s.equalsIgnoreCase(required));
            if (!found) {
                missing.add(required);
            }
        }
        return missing;
    }

    // ==================== 私有验证方法 ====================

    private void requireField(Map<String, String> formData, String fieldKey,
                               String fieldName, List<ValidationError> errors) {
        String value = formData.get(fieldKey);
        if (value == null || value.trim().isEmpty()) {
            errors.add(new ValidationError(fieldKey, fieldName,
                    fieldName + "为必填字段，不能为空", null));
        }
    }

    private void validateEmployeeId(String employeeId, List<ValidationError> errors) {
        if (employeeId != null && !employeeId.isEmpty()
                && !EMPLOYEE_ID_PATTERN.matcher(employeeId).matches()) {
            errors.add(new ValidationError("employeeId", "员工ID",
                    "员工ID格式不正确，应为字母开头加数字", "例如：EMP001"));
        }
    }

    private void validateAmount(String amount, String fieldName, List<ValidationError> errors) {
        if (amount != null && !amount.isEmpty()) {
            if (!AMOUNT_PATTERN.matcher(amount).matches()) {
                errors.add(new ValidationError("amount", fieldName,
                        fieldName + "格式不正确，应为正数且最多两位小数", "例如：1500.00"));
            } else {
                BigDecimal value = new BigDecimal(amount);
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(new ValidationError("amount", fieldName,
                            fieldName + "必须大于0", "例如：1500.00"));
                }
            }
        }
    }

    private void validateDate(String date, String fieldName, List<ValidationError> errors) {
        if (date != null && !date.isEmpty()) {
            try {
                LocalDate parsed = LocalDate.parse(date, DATE_FORMAT);
                if (parsed.isAfter(LocalDate.now())) {
                    errors.add(new ValidationError("date", fieldName,
                            fieldName + "不能晚于当前日期", "例如：2024-01-15"));
                }
            } catch (DateTimeParseException e) {
                errors.add(new ValidationError("date", fieldName,
                        fieldName + "格式不正确，应为 yyyy-MM-dd", "例如：2024-01-15"));
            }
        }
    }

    private void validateExpenseType(String expenseType, List<ValidationError> errors) {
        if (expenseType != null && !expenseType.isEmpty()) {
            List<String> validTypes = Arrays.asList("差旅费", "交通费", "餐饮费", "办公用品",
                    "通讯费", "培训费", "招待费", "其他");
            if (!validTypes.contains(expenseType)) {
                errors.add(new ValidationError("expenseType", "报销类型",
                        "报销类型不在允许范围内",
                        "允许的类型：差旅费、交通费、餐饮费、办公用品、通讯费、培训费、招待费、其他"));
            }
        }
    }
}
