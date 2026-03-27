package com.company.finance.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AgentConfig 单元测试。
 * <p>
 * 验证各业务智能体的指令配置完整性和关键内容。
 * ReactAgent 的实际构建依赖 AI-Nova 框架运行时，
 * 此处仅验证配置类的指令常量正确性。
 * </p>
 */
class AgentConfigTest {

    @Test
    void expenseInstruction_containsKeyResponsibilities() {
        assertThat(AgentConfig.EXPENSE_INSTRUCTION)
                .contains("报销")
                .contains("查询报销单状态")
                .contains("发起报销单")
                .contains("外部系统调用失败");
    }

    @Test
    void invoiceInstruction_containsKeyResponsibilities() {
        assertThat(AgentConfig.INVOICE_INSTRUCTION)
                .contains("发票")
                .contains("发票验真")
                .contains("发票代码")
                .contains("发票号码");
    }

    @Test
    void salaryInstruction_containsPermissionRequirements() {
        assertThat(AgentConfig.SALARY_INSTRUCTION)
                .contains("薪资")
                .contains("数据权限")
                .contains("只能查询自己的薪资数据")
                .contains("越权查询")
                .contains("您没有权限访问该数据");
    }

    @Test
    void supplierInstruction_containsKeyResponsibilities() {
        assertThat(AgentConfig.SUPPLIER_INSTRUCTION)
                .contains("供应商")
                .contains("ERP")
                .contains("供应商ID")
                .contains("模糊搜索");
    }

    @Test
    void guideInstruction_containsFlowGuidanceCapabilities() {
        assertThat(AgentConfig.GUIDE_INSTRUCTION)
                .contains("退回原因分析")
                .contains("材料补齐引导")
                .contains("表单验证")
                .contains("重新提交");
    }

    @Test
    void allInstructions_useChineseLanguage() {
        assertThat(AgentConfig.EXPENSE_INSTRUCTION).contains("使用中文");
        assertThat(AgentConfig.INVOICE_INSTRUCTION).contains("使用中文");
        assertThat(AgentConfig.SALARY_INSTRUCTION).contains("使用中文");
        assertThat(AgentConfig.SUPPLIER_INSTRUCTION).contains("使用中文");
        assertThat(AgentConfig.GUIDE_INSTRUCTION).contains("使用中文");
    }
}
