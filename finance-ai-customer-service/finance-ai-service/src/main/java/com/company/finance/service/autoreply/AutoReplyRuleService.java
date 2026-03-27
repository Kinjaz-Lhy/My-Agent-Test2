package com.company.finance.service.autoreply;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * 自动回复规则配置服务
 * <p>
 * 支持运营人员配置自动回复规则和快捷回复模板。
 * 当用户消息匹配规则中的关键词时，返回预设的回复内容。
 * </p>
 * <p>
 * 规则存储在内存中（ConcurrentHashMap），支持并发读写。
 * 生产环境可扩展为数据库持久化。
 * </p>
 *
 * @see <a href="需求 6.6">自动回复规则配置</a>
 */
@Service
public class AutoReplyRuleService {

    private static final Logger log = LoggerFactory.getLogger(AutoReplyRuleService.class);

    /** 规则存储：ruleId → AutoReplyRule */
    private final Map<String, AutoReplyRule> ruleStore = new ConcurrentHashMap<>();

    /**
     * 添加自动回复规则
     *
     * @param rule 规则对象（ruleId 为空时自动生成）
     * @return 添加后的规则（含生成的 ruleId）
     */
    public AutoReplyRule addRule(AutoReplyRule rule) {
        if (rule.getRuleId() == null || rule.getRuleId().isEmpty()) {
            rule.setRuleId(UUID.randomUUID().toString());
        }
        LocalDateTime now = LocalDateTime.now();
        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(now);
        }
        rule.setUpdatedAt(now);

        ruleStore.put(rule.getRuleId(), rule);
        log.info("添加自动回复规则: ruleId={}, name={}", rule.getRuleId(), rule.getName());
        return rule;
    }

    /**
     * 更新自动回复规则
     *
     * @param rule 更新后的规则
     * @return 更新后的规则，规则不存在时返回 null
     */
    public AutoReplyRule updateRule(AutoReplyRule rule) {
        if (rule.getRuleId() == null || !ruleStore.containsKey(rule.getRuleId())) {
            return null;
        }
        rule.setUpdatedAt(LocalDateTime.now());
        ruleStore.put(rule.getRuleId(), rule);
        log.info("更新自动回复规则: ruleId={}", rule.getRuleId());
        return rule;
    }

    /**
     * 删除自动回复规则
     *
     * @param ruleId 规则 ID
     * @return 是否成功删除
     */
    public boolean deleteRule(String ruleId) {
        AutoReplyRule removed = ruleStore.remove(ruleId);
        if (removed != null) {
            log.info("删除自动回复规则: ruleId={}", ruleId);
            return true;
        }
        return false;
    }

    /**
     * 获取所有规则（按优先级排序）
     *
     * @return 规则列表
     */
    public List<AutoReplyRule> getAllRules() {
        return ruleStore.values().stream()
                .sorted(Comparator.comparingInt(AutoReplyRule::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * 根据规则 ID 获取规则
     *
     * @param ruleId 规则 ID
     * @return 规则对象，不存在时返回 null
     */
    public AutoReplyRule getRuleById(String ruleId) {
        return ruleStore.get(ruleId);
    }

    /**
     * 匹配用户消息，返回最高优先级匹配的自动回复内容
     * <p>
     * 按优先级顺序遍历所有启用的规则，检查用户消息是否匹配规则的关键词或正则模式。
     * 支持两种匹配方式：
     * <ul>
     *   <li>关键词匹配：用户消息包含规则中的任一关键词</li>
     *   <li>正则模式匹配：用户消息匹配规则中的正则表达式</li>
     * </ul>
     * 当多个规则匹配时，返回优先级最高（priority 数值最小）的规则的回复模板。
     * </p>
     *
     * @param userMessage 用户消息
     * @return 匹配的回复内容，无匹配时返回 null
     */
    public String matchReply(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return null;
        }

        return getAllRules().stream()
                .filter(AutoReplyRule::isEnabled)
                .filter(rule -> matchesRule(rule, userMessage))
                .findFirst()
                .map(AutoReplyRule::getReplyTemplate)
                .orElse(null);
    }

    /**
     * 检查用户消息是否匹配指定规则
     */
    private boolean matchesRule(AutoReplyRule rule, String userMessage) {
        // 关键词匹配
        boolean keywordMatch = rule.getKeywords() != null && !rule.getKeywords().isEmpty()
                && rule.getKeywords().stream().anyMatch(keyword -> userMessage.contains(keyword));
        if (keywordMatch) {
            return true;
        }

        // 正则模式匹配
        if (rule.getPattern() != null && !rule.getPattern().isEmpty()) {
            try {
                return Pattern.compile(rule.getPattern()).matcher(userMessage).find();
            } catch (PatternSyntaxException e) {
                log.warn("规则 {} 的正则表达式无效: {}", rule.getRuleId(), rule.getPattern());
                return false;
            }
        }

        return false;
    }

    /**
     * 获取启用的规则数量
     */
    public long getEnabledRuleCount() {
        return ruleStore.values().stream()
                .filter(AutoReplyRule::isEnabled)
                .count();
    }
}
