package com.company.finance.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 会话重命名请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionRenameRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;
}
