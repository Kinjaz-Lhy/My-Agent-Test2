package com.company.finance.agent.validation;

import java.util.Objects;

/**
 * 表单验证错误
 * <p>
 * 包含错误字段的 key、显示名称、错误描述和正确填写示例。
 * </p>
 */
public class ValidationError {

    private final String fieldKey;
    private final String fieldName;
    private final String errorMessage;
    private final String example;

    public ValidationError(String fieldKey, String fieldName, String errorMessage, String example) {
        this.fieldKey = fieldKey;
        this.fieldName = fieldName;
        this.errorMessage = errorMessage;
        this.example = example;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getExample() {
        return example;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationError that = (ValidationError) o;
        return Objects.equals(fieldKey, that.fieldKey)
                && Objects.equals(fieldName, that.fieldName)
                && Objects.equals(errorMessage, that.errorMessage)
                && Objects.equals(example, that.example);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldKey, fieldName, errorMessage, example);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(fieldName).append(": ").append(errorMessage);
        if (example != null && !example.isEmpty()) {
            sb.append(" (").append(example).append(")");
        }
        return sb.toString();
    }
}
