package com.company.finance.service.autoreply;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动回复规则
 * <p>
 * 运营人员配置的自动回复规则，当用户消息匹配关键词或正则模式时自动回复预设内容。
 * 支持两种匹配方式：关键词列表（子串匹配）和正则表达式模式匹配。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoReplyRule {

    /** 规则唯一标识 */
    private String ruleId;

    /** 规则名称 */
    private String name;

    /** 匹配关键词列表（子串匹配） */
    private List<String> keywords;

    /** 匹配正则表达式模式（可选，与 keywords 二选一或同时使用） */
    private String pattern;

    /** 自动回复内容模板 */
    private String replyTemplate;

    /** 是否启用 */
    private boolean enabled;

    /** 优先级（数值越小优先级越高） */
    private int priority;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
