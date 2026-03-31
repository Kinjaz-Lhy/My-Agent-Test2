package com.company.finance.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 SKILL.md 技能文件能被正确加载，且内容包含预期的关键知识。
 */
class SkillLoadingTest {

    @ParameterizedTest(name = "技能文件存在且非空: {0}")
    @ValueSource(strings = {
            "skills/expense-policy/SKILL.md",
            "skills/invoice-guide/SKILL.md",
            "skills/tax-policy/SKILL.md",
            "skills/approval-flow/SKILL.md"
    })
    void skillFile_existsAndNotEmpty(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        assertThat(resource.exists()).as("文件应存在: %s", path).isTrue();

        String content = readResource(resource);
        assertThat(content).as("文件内容不应为空: %s", path).isNotBlank();
    }

    @Test
    void expensePolicySkill_containsKeyKnowledge() throws IOException {
        String content = readSkill("skills/expense-policy/SKILL.md");
        assertThat(content)
                .contains("差旅住宿标准")
                .contains("餐饮补贴标准")
                .contains("报销时效");
    }

    @Test
    void invoiceGuideSkill_containsKeyKnowledge() throws IOException {
        String content = readSkill("skills/invoice-guide/SKILL.md");
        assertThat(content)
                .contains("增值税专用发票")
                .contains("发票验真方法")
                .contains("发票报销注意事项");
    }

    @Test
    void taxPolicySkill_containsKeyKnowledge() throws IOException {
        String content = readSkill("skills/tax-policy/SKILL.md");
        assertThat(content)
                .contains("个人所得税")
                .contains("税率表")
                .contains("专项附加扣除");
    }

    @Test
    void approvalFlowSkill_containsKeyKnowledge() throws IOException {
        String content = readSkill("skills/approval-flow/SKILL.md");
        assertThat(content)
                .contains("报销单审批流程")
                .contains("借款单审批流程")
                .contains("付款申请审批流程");
    }

    private String readSkill(String path) throws IOException {
        return readResource(new ClassPathResource(path));
    }

    private String readResource(ClassPathResource resource) throws IOException {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                sb.append(buf, 0, len);
            }
            return sb.toString();
        }
    }
}
