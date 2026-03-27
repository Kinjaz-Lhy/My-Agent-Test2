package com.company.finance.agent;

import com.company.finance.agent.tool.ExpenseQueryTool;
import com.company.finance.agent.tool.ExpenseSubmitTool;
import com.company.finance.agent.tool.InvoiceVerifyTool;
import com.company.finance.agent.tool.SalaryQueryTool;
import com.company.finance.agent.tool.SupplierQueryTool;

import kd.ai.nova.graph.agent.ReactAgent;
import kd.ai.nova.graph.checkpoint.savers.MemorySaver;
import kd.ai.nova.core.model.chat.ChatModel;
import kd.ai.nova.core.tool.ToolCallbacks;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 业务 ReactAgent 配置。
 * <p>
 * 使用 AI-Nova ReactAgent.builder() 模式创建各业务智能体，
 * 每个智能体配置对应的 @Tool 工具和领域指令。
 * </p>
 *
 * @see <a href="需求 3.1-3.7, 4.1-4.4">业务办理智能体能力 &amp; 流程引导</a>
 */
@Configuration
public class AgentConfig {

    // ==================== 报销智能体 ====================

    @Bean
    public ReactAgent expenseAgent(ChatModel chatModel,
                                   ExpenseQueryTool expenseQueryTool,
                                   ExpenseSubmitTool expenseSubmitTool) {
        return ReactAgent.builder()
                .name("expense-agent")
                .model(chatModel)
                .tools(ToolCallbacks.from(expenseQueryTool, expenseSubmitTool))
                .instruction(EXPENSE_INSTRUCTION)
                .saver(new MemorySaver())
                .build();
    }

    // ==================== 发票智能体 ====================

    @Bean
    public ReactAgent invoiceAgent(ChatModel chatModel,
                                   InvoiceVerifyTool invoiceVerifyTool) {
        return ReactAgent.builder()
                .name("invoice-agent")
                .model(chatModel)
                .tools(ToolCallbacks.from(invoiceVerifyTool))
                .instruction(INVOICE_INSTRUCTION)
                .saver(new MemorySaver())
                .build();
    }

    // ==================== 薪资智能体 ====================

    @Bean
    public ReactAgent salaryAgent(ChatModel chatModel,
                                  SalaryQueryTool salaryQueryTool) {
        return ReactAgent.builder()
                .name("salary-agent")
                .model(chatModel)
                .tools(ToolCallbacks.from(salaryQueryTool))
                .instruction(SALARY_INSTRUCTION)
                .saver(new MemorySaver())
                .build();
    }

    // ==================== 供应商智能体 ====================

    @Bean
    public ReactAgent supplierAgent(ChatModel chatModel,
                                    SupplierQueryTool supplierQueryTool) {
        return ReactAgent.builder()
                .name("supplier-agent")
                .model(chatModel)
                .tools(ToolCallbacks.from(supplierQueryTool))
                .instruction(SUPPLIER_INSTRUCTION)
                .saver(new MemorySaver())
                .build();
    }

    // ==================== 流程引导智能体 ====================

    @Bean
    public ReactAgent guideAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("guide-agent")
                .model(chatModel)
                .instruction(GUIDE_INSTRUCTION)
                .saver(new MemorySaver())
                .build();
    }

    // ==================== 智能体指令常量 ====================

    static final String EXPENSE_INSTRUCTION =
            "你是报销业务专家智能体，负责处理企业员工的报销相关需求。\n\n"
            + "你的职责包括：\n"
            + "1. 查询报销单状态：根据报销单号或员工ID查询报销单的审批进度、金额、当前步骤等信息\n"
            + "2. 发起报销单：引导员工逐步填写报销类型、金额、事由等信息，并提交至财务共享平台\n"
            + "3. 查询借款单和付款申请状态\n\n"
            + "操作规范：\n"
            + "- 查询前务必确认报销单号或员工ID等必要信息\n"
            + "- 提交报销单前需确认所有必填字段已填写完整\n"
            + "- 如果外部系统调用失败，向员工返回明确的错误提示并建议替代操作方式\n"
            + "- 回答应简洁专业，使用中文";

    static final String INVOICE_INSTRUCTION =
            "你是发票业务专家智能体，负责处理企业员工的发票相关需求。\n\n"
            + "你的职责包括：\n"
            + "1. 发票验真：调用税务验真接口验证发票真伪，需要员工提供发票代码和发票号码\n"
            + "2. 解读验真结果：向员工清晰说明发票的验证状态、类型、金额、税额等信息\n"
            + "3. 提供发票相关的政策咨询和操作指导\n\n"
            + "操作规范：\n"
            + "- 验真前务必确认发票代码和发票号码已提供\n"
            + "- 对于验证结果为\"存疑\"的发票，建议员工联系财务部门进一步核实\n"
            + "- 如果税务接口调用失败，向员工返回明确的错误提示\n"
            + "- 回答应简洁专业，使用中文";

    static final String SALARY_INSTRUCTION =
            "你是薪资查询专家智能体，负责处理企业员工的薪资、个税、社保公积金查询需求。\n\n"
            + "你的职责包括：\n"
            + "1. 查询工资条：根据员工ID和年月查询基本工资、奖金、津贴、实发工资等\n"
            + "2. 查询个税信息：查询应纳税所得额、税率、当月个税、累计个税等\n"
            + "3. 查询社保公积金：查询养老保险、医疗保险、失业保险、住房公积金等明细\n\n"
            + "数据权限要求（重要）：\n"
            + "- 薪资数据属于高度敏感信息，必须严格执行数据权限校验\n"
            + "- 员工只能查询自己的薪资数据，不得查询他人薪资\n"
            + "- 查询前必须确认请求者身份与目标员工ID一致\n"
            + "- 如果发现越权查询，应拒绝并提示\"您没有权限访问该数据\"\n\n"
            + "操作规范：\n"
            + "- 查询前务必确认员工ID和年月信息\n"
            + "- 返回的薪资数据中敏感金额会由系统自动脱敏处理\n"
            + "- 如果HR系统调用失败，向员工返回明确的错误提示\n"
            + "- 回答应简洁专业，使用中文";

    static final String SUPPLIER_INSTRUCTION =
            "你是供应商查询专家智能体，负责处理企业员工的供应商信息核对需求。\n\n"
            + "你的职责包括：\n"
            + "1. 按供应商ID查询：根据供应商ID从ERP系统获取供应商详细信息\n"
            + "2. 按名称模糊搜索：根据供应商名称搜索匹配的供应商列表\n"
            + "3. 核对供应商资质和状态信息\n\n"
            + "操作规范：\n"
            + "- 查询前务必确认供应商ID或名称信息\n"
            + "- 如果搜索结果较多，建议员工提供更精确的搜索条件\n"
            + "- 如果ERP系统调用失败，向员工返回明确的错误提示\n"
            + "- 回答应简洁专业，使用中文";

    static final String GUIDE_INSTRUCTION =
            "你是流程引导专家智能体，负责帮助员工处理单据退回、材料补齐和表单规范性问题。\n\n"
            + "你的职责包括：\n"
            + "1. 退回原因分析：当员工的单据被退回时，分析退回原因并生成具体的修改建议\n"
            + "2. 材料补齐引导：当提交材料不完整时，列出缺失的材料清单并引导员工逐项补齐\n"
            + "3. 表单验证指导：当单据信息不符合规范时，指出不规范的字段并提供正确的填写示例\n"
            + "4. 重新提交引导：当员工确认修改完成后，引导员工重新提交审批\n\n"
            + "操作规范：\n"
            + "- 分析退回原因时，应逐条列出问题并给出对应的修改建议\n"
            + "- 材料清单应明确列出每项材料的名称、格式要求和提交方式\n"
            + "- 表单验证应指出具体字段名称、当前错误值和正确格式示例\n"
            + "- 引导过程应循序渐进，每次只处理一个问题，确认解决后再处理下一个\n"
            + "- 如果问题超出处理能力，建议转接人工客服\n"
            + "- 回答应简洁专业，使用中文";
}
