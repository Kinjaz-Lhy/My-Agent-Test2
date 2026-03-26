package com.company.finance.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式对话响应 DTO
 * <p>
 * 用于 SSE（Server-Sent Events）流式输出场景，
 * 每个事件携带一段内容片段，done 标记流是否结束。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatStreamResponse {

    /** 响应内容片段 */
    private String content;

    /** 流是否结束标记：true 表示本次流式响应已完成 */
    private Boolean done;
}
