package com.company.finance.agent.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FormValidationService 单元测试。
 */
class FormValidationServiceTest {

    private FormValidationService service;

    @BeforeEach
    void setUp() {
        service = new FormValidationService();
    }

    @Nested
    class ExpenseFormValidation {

        @Test
        void validForm_returnsNoErrors() {
            Map<String, String> form = validExpenseForm();
            List<ValidationError> errors = service.validateExpenseForm(form);
            assertThat(errors).isEmpty();
        }

        @Test
        void missingRequiredFields_returnsErrors() {
            Map<String, String> form = new HashMap<>();
            List<ValidationError> errors = service.validateExpenseForm(form);
            assertThat(errors).isNotEmpty();
            assertThat(errors.stream().map(ValidationError::getFieldKey))
                    .contains("employeeId", "expenseType", "amount", "reason", "date");
        }

        @Test
        void invalidEmployeeIdFormat_returnsError() {
            Map<String, String> form = validExpenseForm();
            form.put("employeeId", "12345");
            List<ValidationError> errors = service.validateExpenseForm(form);
            assertThat(errors.stream().map(ValidationError::getFieldKey))
                    .contains("employeeId");
        }

        @Test
        void invalidAmountFormat_returnsError() {
            Map<String, String> form = validExpenseForm();
            form.put("amount", "abc");
            List<ValidationError> errors = service.validateExpenseForm(form);
            assertThat(errors.stream().map(ValidationError::getFieldKey))
                    .contains("amount");
        }

        @Test
        void invalidDateFormat_returnsError() {
            Map<String, String> form = validExpenseForm();
            form.put("date", "2024/01/15");
            List<ValidationError> errors = service.validateExpenseForm(form);
            assertThat(errors.stream().map(ValidationError::getFieldKey))
                    .contains("date");
        }

        @Test
        void futureDate_returnsError() {
            Map<String, String> form = validExpenseForm();
            form.put("date", LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            List<ValidationError> errors = service.validateExpenseForm(form);
            assertThat(errors.stream().map(ValidationError::getFieldKey))
                    .contains("date");
        }

        @Test
        void invalidExpenseType_returnsError() {
            Map<String, String> form = validExpenseForm();
            form.put("expenseType", "无效类型");
            List<ValidationError> errors = service.validateExpenseForm(form);
            assertThat(errors.stream().map(ValidationError::getFieldKey))
                    .contains("expenseType");
        }

        @Test
        void emptyField_treatedAsMissing() {
            Map<String, String> form = validExpenseForm();
            form.put("reason", "  ");
            List<ValidationError> errors = service.validateExpenseForm(form);
            assertThat(errors.stream().map(ValidationError::getFieldKey))
                    .contains("reason");
        }

        private Map<String, String> validExpenseForm() {
            Map<String, String> form = new HashMap<>();
            form.put("employeeId", "EMP001");
            form.put("expenseType", "差旅费");
            form.put("amount", "1500.00");
            form.put("reason", "出差北京");
            form.put("date", "2024-01-15");
            return form;
        }
    }

    @Nested
    class InvoiceFormValidation {

        @Test
        void validForm_returnsNoErrors() {
            Map<String, String> form = new HashMap<>();
            form.put("invoiceCode", "1100192130");
            form.put("invoiceNumber", "04177562");
            List<ValidationError> errors = service.validateInvoiceForm(form);
            assertThat(errors).isEmpty();
        }

        @Test
        void missingInvoiceCode_returnsError() {
            Map<String, String> form = new HashMap<>();
            form.put("invoiceNumber", "04177562");
            List<ValidationError> errors = service.validateInvoiceForm(form);
            assertThat(errors.stream().map(ValidationError::getFieldKey))
                    .contains("invoiceCode");
        }

        @Test
        void invalidInvoiceCodeFormat_returnsError() {
            Map<String, String> form = new HashMap<>();
            form.put("invoiceCode", "ABC");
            form.put("invoiceNumber", "04177562");
            List<ValidationError> errors = service.validateInvoiceForm(form);
            assertThat(errors.stream().map(ValidationError::getFieldKey))
                    .contains("invoiceCode");
        }

        @Test
        void invalidInvoiceNumberFormat_returnsError() {
            Map<String, String> form = new HashMap<>();
            form.put("invoiceCode", "1100192130");
            form.put("invoiceNumber", "123");
            List<ValidationError> errors = service.validateInvoiceForm(form);
            assertThat(errors.stream().map(ValidationError::getFieldKey))
                    .contains("invoiceNumber");
        }
    }

    @Nested
    class RejectionReasonAnalysis {

        @Test
        void amountRelatedRejection_suggestsAmountCheck() {
            List<String> suggestions = service.analyzeRejectionReason("报销金额超标");
            assertThat(suggestions).anyMatch(s -> s.contains("金额"));
        }

        @Test
        void invoiceRelatedRejection_suggestsInvoiceCheck() {
            List<String> suggestions = service.analyzeRejectionReason("发票不清晰");
            assertThat(suggestions).anyMatch(s -> s.contains("发票"));
        }

        @Test
        void materialRelatedRejection_suggestsMaterialCheck() {
            List<String> suggestions = service.analyzeRejectionReason("缺少证明材料");
            assertThat(suggestions).anyMatch(s -> s.contains("材料") || s.contains("附件"));
        }

        @Test
        void nullRejection_suggestsContactApprover() {
            List<String> suggestions = service.analyzeRejectionReason(null);
            assertThat(suggestions).anyMatch(s -> s.contains("联系"));
        }

        @Test
        void unknownRejection_includesOriginalReason() {
            List<String> suggestions = service.analyzeRejectionReason("其他未知原因XYZ");
            assertThat(suggestions).anyMatch(s -> s.contains("其他未知原因XYZ"));
        }
    }

    @Nested
    class MissingMaterialsCheck {

        @Test
        void allMaterialsSubmitted_returnsEmpty() {
            List<String> submitted = Arrays.asList("身份证复印件", "发票原件", "行程单");
            List<String> required = Arrays.asList("身份证复印件", "发票原件", "行程单");
            List<String> missing = service.checkMissingMaterials(submitted, required);
            assertThat(missing).isEmpty();
        }

        @Test
        void someMaterialsMissing_returnsMissingList() {
            List<String> submitted = Arrays.asList("身份证复印件");
            List<String> required = Arrays.asList("身份证复印件", "发票原件", "行程单");
            List<String> missing = service.checkMissingMaterials(submitted, required);
            assertThat(missing).containsExactly("发票原件", "行程单");
        }

        @Test
        void noMaterialsSubmitted_returnsAllRequired() {
            List<String> submitted = Collections.emptyList();
            List<String> required = Arrays.asList("身份证复印件", "发票原件");
            List<String> missing = service.checkMissingMaterials(submitted, required);
            assertThat(missing).containsExactlyElementsOf(required);
        }
    }

    @Nested
    class ValidationErrorFormat {

        @Test
        void errorToString_includesFieldNameAndMessage() {
            ValidationError error = new ValidationError("amount", "报销金额",
                    "报销金额格式不正确", "例如：1500.00");
            String str = error.toString();
            assertThat(str).contains("报销金额").contains("格式不正确").contains("1500.00");
        }

        @Test
        void errorToString_withoutExample() {
            ValidationError error = new ValidationError("reason", "报销事由",
                    "报销事由为必填字段", null);
            String str = error.toString();
            assertThat(str).contains("报销事由").contains("必填字段");
        }
    }
}
