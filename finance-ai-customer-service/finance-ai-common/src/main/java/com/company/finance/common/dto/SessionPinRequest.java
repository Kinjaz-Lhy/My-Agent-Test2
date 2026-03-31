package com.company.finance.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 会话置顶请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionPinRequest {

    @NotNull(message = "置顶状态不能为空")
    private Boolean pinned;
}
