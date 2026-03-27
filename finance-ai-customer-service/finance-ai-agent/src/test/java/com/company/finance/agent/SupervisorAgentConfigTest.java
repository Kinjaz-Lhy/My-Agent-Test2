package com.company.finance.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SupervisorAgentConfig 单元测试。
 * <p>
 * 验证调度器指令配置的完整性和关键内容。
 * SupervisorAgent 的实际构建依赖 AI-Nova 框架运行时，
 * 此处仅验证配置类的指令常量正确性。
 * </p>
 */
class SupervisorAgentConfigTest {

    @Test
    void supervisorInstruction_containsAllAgentNames() {
        assertThat(SupervisorAgentConfig.SUPERVISOR_INSTRUCTION)
                .contains("expense-agent")
                .contains("invoice-agent")
                .contains("salary-agent")
                .contains("supplier-agent")
                .contains("guide-agent");
    }

    @Test
    void supervisorInstruction_containsDispatchRules() {
        assertThat(SupervisorAgentConfig.SUPERVISOR_INSTRUCTION)
                .contains("报销")
                .contains("发票验真")
                .contains("工资")
                .contains("供应商")
                .contains("单据退回");
    }

    @Test
    void supervisorInstruction_containsFallbackGuidance() {
        assertThat(SupervisorAgentConfig.SUPERVISOR_INSTRUCTION)
                .contains("意图不明确")
                .contains("人工客服");
    }

    @Test
    void supervisorInstruction_usesChineseLanguage() {
        assertThat(SupervisorAgentConfig.SUPERVISOR_INSTRUCTION)
                .contains("使用中文");
    }
}
