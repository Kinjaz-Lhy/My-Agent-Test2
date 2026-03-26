package com.company.finance.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一错误响应 DTO
 * <p>
 * 用于全局异常处理返回标准化的错误信息，
 * 包含错误码和可读的错误描述。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** 错误码，参见 {@link com.company.finance.common.constants.ErrorCode} */
    private String code;

    /** 错误描述信息 */
    private String message;
}
