package com.company.finance.service.autoreply;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AutoReplyRuleService 单元测试
 * <p>
 * 验证自动回复规则的 CRUD 操作和匹配逻辑。
 * </p>
 */
class AutoReplyRuleServiceTest {

    private AutoReplyRuleService service;

    @BeforeEach
    void setUp() {
        service = new AutoReplyRuleService();
    }

    // ========== 添加规则测试 ==========

    @Test
    @DisplayName("addRule 应自动生成 ruleId 并设置时间戳")
    void addRule_shouldGenerateRuleIdAndTimestamps() {
        AutoReplyRule rule = AutoReplyRule.builder()
                .name("报销咨询")
                .keywords(Arrays.asList("报销", "报销单"))
                .replyTemplate("请提供您的报销单号，我来帮您查询。")
                .enabled(true)
                .priority(1)
                .build();

        AutoReplyRule result = service.addRule(rule);

        assertThat(result.getRuleId()).isNotNull().isNotEmpty();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getName()).isEqualTo("报销咨询");
    }

    @Test
    @DisplayName("addRule 应保留已有的 ruleId")
    void addRule_shouldPreserveExistingRuleId() {
        AutoReplyRule rule = AutoReplyRule.builder()
                .ruleId("custom-id-001")
                .name("测试规则")
                .keywords(Collections.singletonList("测试"))
                .replyTemplate("测试回复")
                .enabled(true)
                .priority(1)
                .build();

        AutoReplyRule result = service.addRule(rule);

        assertThat(result.getRuleId()).isEqualTo("custom-id-001");
    }

    // ========== 更新规则测试 ==========

    @Test
    @DisplayName("updateRule 应更新已有规则并刷新 updatedAt")
    void updateRule_shouldUpdateExistingRule() {
        AutoReplyRule rule = service.addRule(AutoReplyRule.builder()
                .name("原始规则")
                .keywords(Collections.singletonList("原始"))
                .replyTemplate("原始回复")
                .enabled(true)
                .priority(1)
                .build());

        rule.setName("更新后规则");
        rule.setReplyTemplate("更新后回复");
        AutoReplyRule updated = service.updateRule(rule);

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("更新后规则");
        assertThat(updated.getReplyTemplate()).isEqualTo("更新后回复");
    }

    @Test
    @DisplayName("updateRule 规则不存在时应返回 null")
    void updateRule_shouldReturnNullForNonExistentRule() {
        AutoReplyRule rule = AutoReplyRule.builder()
                .ruleId("non-existent")
                .name("不存在的规则")
                .build();

        AutoReplyRule result = service.updateRule(rule);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("updateRule ruleId 为 null 时应返回 null")
    void updateRule_shouldReturnNullWhenRuleIdIsNull() {
        AutoReplyRule rule = AutoReplyRule.builder().name("无ID规则").build();

        AutoReplyRule result = service.updateRule(rule);

        assertThat(result).isNull();
    }

    // ========== 删除规则测试 ==========

    @Test
    @DisplayName("deleteRule 应成功删除已有规则")
    void deleteRule_shouldRemoveExistingRule() {
        AutoReplyRule rule = service.addRule(AutoReplyRule.builder()
                .name("待删除规则")
                .keywords(Collections.singletonList("删除"))
                .replyTemplate("回复")
                .enabled(true)
                .priority(1)
                .build());

        boolean deleted = service.deleteRule(rule.getRuleId());

        assertThat(deleted).isTrue();
        assertThat(service.getRuleById(rule.getRuleId())).isNull();
    }

    @Test
    @DisplayName("deleteRule 规则不存在时应返回 false")
    void deleteRule_shouldReturnFalseForNonExistentRule() {
        boolean deleted = service.deleteRule("non-existent");

        assertThat(deleted).isFalse();
    }

    // ========== 查询规则测试 ==========

    @Test
    @DisplayName("getAllRules 应按优先级升序排序")
    void getAllRules_shouldReturnSortedByPriority() {
        service.addRule(AutoReplyRule.builder()
                .name("低优先级").keywords(Collections.singletonList("低"))
                .replyTemplate("低").enabled(true).priority(10).build());
        service.addRule(AutoReplyRule.builder()
                .name("高优先级").keywords(Collections.singletonList("高"))
                .replyTemplate("高").enabled(true).priority(1).build());
        service.addRule(AutoReplyRule.builder()
                .name("中优先级").keywords(Collections.singletonList("中"))
                .replyTemplate("中").enabled(true).priority(5).build());

        List<AutoReplyRule> rules = service.getAllRules();

        assertThat(rules).hasSize(3);
        assertThat(rules.get(0).getName()).isEqualTo("高优先级");
        assertThat(rules.get(1).getName()).isEqualTo("中优先级");
        assertThat(rules.get(2).getName()).isEqualTo("低优先级");
    }

    @Test
    @DisplayName("getRuleById 应返回对应规则")
    void getRuleById_shouldReturnCorrectRule() {
        AutoReplyRule rule = service.addRule(AutoReplyRule.builder()
                .name("查询测试").keywords(Collections.singletonList("查询"))
                .replyTemplate("回复").enabled(true).priority(1).build());

        AutoReplyRule found = service.getRuleById(rule.getRuleId());

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("查询测试");
    }

    // ========== 关键词匹配测试 ==========

    @Test
    @DisplayName("matchReply 应通过关键词匹配返回回复内容")
    void matchReply_shouldMatchByKeyword() {
        service.addRule(AutoReplyRule.builder()
                .name("报销规则")
                .keywords(Arrays.asList("报销", "报销单"))
                .replyTemplate("请提供报销单号。")
                .enabled(true)
                .priority(1)
                .build());

        String reply = service.matchReply("我想查询报销单状态");

        assertThat(reply).isEqualTo("请提供报销单号。");
    }

    @Test
    @DisplayName("matchReply 多规则匹配时应返回最高优先级的回复")
    void matchReply_shouldReturnHighestPriorityMatch() {
        service.addRule(AutoReplyRule.builder()
                .name("低优先级报销").keywords(Collections.singletonList("报销"))
                .replyTemplate("低优先级回复").enabled(true).priority(10).build());
        service.addRule(AutoReplyRule.builder()
                .name("高优先级报销").keywords(Collections.singletonList("报销"))
                .replyTemplate("高优先级回复").enabled(true).priority(1).build());

        String reply = service.matchReply("报销查询");

        assertThat(reply).isEqualTo("高优先级回复");
    }

    @Test
    @DisplayName("matchReply 无匹配时应返回 null")
    void matchReply_shouldReturnNullWhenNoMatch() {
        service.addRule(AutoReplyRule.builder()
                .name("报销规则").keywords(Collections.singletonList("报销"))
                .replyTemplate("回复").enabled(true).priority(1).build());

        String reply = service.matchReply("发票验真");

        assertThat(reply).isNull();
    }

    @Test
    @DisplayName("matchReply 禁用规则不应被匹配")
    void matchReply_shouldNotMatchDisabledRules() {
        service.addRule(AutoReplyRule.builder()
                .name("禁用规则").keywords(Collections.singletonList("报销"))
                .replyTemplate("不应返回").enabled(false).priority(1).build());

        String reply = service.matchReply("报销查询");

        assertThat(reply).isNull();
    }

    @Test
    @DisplayName("matchReply 用户消息为 null 时应返回 null")
    void matchReply_shouldReturnNullForNullMessage() {
        assertThat(service.matchReply(null)).isNull();
    }

    @Test
    @DisplayName("matchReply 用户消息为空字符串时应返回 null")
    void matchReply_shouldReturnNullForEmptyMessage() {
        assertThat(service.matchReply("")).isNull();
    }

    // ========== 正则模式匹配测试 ==========

    @Test
    @DisplayName("matchReply 应通过正则模式匹配返回回复内容")
    void matchReply_shouldMatchByPattern() {
        service.addRule(AutoReplyRule.builder()
                .name("发票号匹配")
                .pattern("\\d{8,12}")
                .replyTemplate("检测到发票号码，正在为您验真。")
                .enabled(true)
                .priority(1)
                .build());

        String reply = service.matchReply("请帮我验证发票号12345678");

        assertThat(reply).isEqualTo("检测到发票号码，正在为您验真。");
    }

    @Test
    @DisplayName("matchReply 无效正则模式不应导致异常")
    void matchReply_shouldHandleInvalidPatternGracefully() {
        service.addRule(AutoReplyRule.builder()
                .name("无效正则")
                .pattern("[invalid")
                .replyTemplate("不应返回")
                .enabled(true)
                .priority(1)
                .build());

        String reply = service.matchReply("任意消息");

        assertThat(reply).isNull();
    }

    @Test
    @DisplayName("matchReply 关键词和模式同时配置时关键词优先匹配")
    void matchReply_keywordMatchTakesPrecedenceOverPattern() {
        service.addRule(AutoReplyRule.builder()
                .name("混合规则")
                .keywords(Collections.singletonList("报销"))
                .pattern(".*发票.*")
                .replyTemplate("混合匹配回复")
                .enabled(true)
                .priority(1)
                .build());

        // 关键词匹配
        assertThat(service.matchReply("报销查询")).isEqualTo("混合匹配回复");
        // 模式匹配
        assertThat(service.matchReply("发票验真")).isEqualTo("混合匹配回复");
    }

    // ========== 启用规则计数测试 ==========

    @Test
    @DisplayName("getEnabledRuleCount 应返回启用规则数量")
    void getEnabledRuleCount_shouldReturnCorrectCount() {
        service.addRule(AutoReplyRule.builder()
                .name("启用1").keywords(Collections.singletonList("a"))
                .replyTemplate("r").enabled(true).priority(1).build());
        service.addRule(AutoReplyRule.builder()
                .name("禁用1").keywords(Collections.singletonList("b"))
                .replyTemplate("r").enabled(false).priority(2).build());
        service.addRule(AutoReplyRule.builder()
                .name("启用2").keywords(Collections.singletonList("c"))
                .replyTemplate("r").enabled(true).priority(3).build());

        assertThat(service.getEnabledRuleCount()).isEqualTo(2);
    }
}
