package com.company.finance.agent;

import com.company.finance.agent.tool.ExpenseQueryTool;
import com.company.finance.agent.tool.ExpenseSubmitTool;
import com.company.finance.agent.tool.InvoiceVerifyTool;
import com.company.finance.agent.tool.SalaryQueryTool;
import com.company.finance.agent.tool.SupplierQueryTool;

import kd.ai.nova.chat.advisor.SkillAdvisor;
import kd.ai.nova.core.skills.registry.classpath.ClasspathSkillRegistry;
import kd.ai.nova.core.tool.ToolCallback;
import kd.ai.nova.core.tool.ToolCallbacks;
import kd.ai.nova.graph.agent.ReactAgent;
import kd.ai.nova.core.model.chat.ChatModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务 ReactAgent 配置。
 * <p>
 * 使用 AI-Nova ReactAgent.builder() 模式创建各业务智能体。
 * 技能知识通过 ClasspathSkillRegistry + SkillAdvisor 渐进式加载，
 * 模型按需调用 read_skill() 获取完整知识内容，减少初始上下文大小。
 * </p>
 */
@Configuration
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    // ==================== 技能注册中心 ====================

    @Bean
    public ClasspathSkillRegistry skillRegistry() {
        ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder()
                .classpathPath("skills")
                .build();
        log.info("已从 classpath:skills/ 加载技能注册中心");
        return registry;
    }

    // ==================== 技能绑定工具映射 ====================

    @Bean
    public SkillAdvisor skillAdvisor(ClasspathSkillRegistry skillRegistry,
                                     ExpenseQueryTool expenseQueryTool,
                                     ExpenseSubmitTool expenseSubmitTool,
                                     InvoiceVerifyTool invoiceVerifyTool,
                                     SalaryQueryTool salaryQueryTool,
                                     SupplierQueryTool supplierQueryTool) {
        // 技能名称 → 绑定的工具列表
        Map<String, List<ToolCallback>> groupedTools = new HashMap<>();
        groupedTools.put("expense-policy",
                Arrays.<ToolCallback>asList(ToolCallbacks.from(expenseQueryTool, expenseSubmitTool)));
        groupedTools.put("invoice-guide",
                Arrays.<ToolCallback>asList(ToolCallbacks.from(invoiceVerifyTool)));
        groupedTools.put("tax-policy",
                Arrays.<ToolCallback>asList(ToolCallbacks.from(salaryQueryTool)));
        groupedTools.put("approval-flow",
                Arrays.<ToolCallback>asList(ToolCallbacks.from(supplierQueryTool)));

        log.info("已配置技能绑定工具: {}", groupedTools.keySet());

        return SkillAdvisor.builder()
                .skillRegistry(skillRegistry)
                .groupedTools(groupedTools)
                .build();
    }

    // ==================== 报销智能体 ====================

    @Bean
    public ReactAgent expenseAgent(ChatModel chatModel,
                                   ExpenseQueryTool expenseQueryTool,
                                   ExpenseSubmitTool expenseSubmitTool) {
        return ReactAgent.builder()
                .name("expense-agent")
                .description("处理报销单查询、报销单发起、借款单和付款申请查询等报销相关业务")
                .model(chatModel)
                .tools(ToolCallbacks.from(expenseQueryTool, expenseSubmitTool))
                .instruction(EXPENSE_INSTRUCTION)
                .outputKey("agent_output")
                .build();
    }

    // ==================== 发票智能体 ====================

    @Bean
    public ReactAgent invoiceAgent(ChatModel chatModel,
                                   InvoiceVerifyTool invoiceVerifyTool) {
        return ReactAgent.builder()
                .name("invoice-agent")
                .description("处理发票验真、发票查询和发票相关咨询")
                .model(chatModel)
                .tools(ToolCallbacks.from(invoiceVerifyTool))
                .instruction(INVOICE_INSTRUCTION)
                .outputKey("agent_output")
                .build();
    }

    // ==================== 薪资智能体 ====================

    @Bean
    public ReactAgent salaryAgent(ChatModel chatModel,
                                  SalaryQueryTool salaryQueryTool) {
        return ReactAgent.builder()
                .name("salary-agent")
                .description("处理工资条查询、个税查询、社保公积金查询等薪资相关业务")
                .model(chatModel)
                .tools(ToolCallbacks.from(salaryQueryTool))
                .instruction(SALARY_INSTRUCTION)
                .outputKey("agent_output")
                .build();
    }

    // ==================== 供应商智能体 ====================

    @Bean
    public ReactAgent supplierAgent(ChatModel chatModel,
                                    SupplierQueryTool supplierQueryTool) {
        return ReactAgent.builder()
                .name("supplier-agent")
                .description("处理供应商信息核对、供应商搜索等供应商相关业务")
                .model(chatModel)
                .tools(ToolCallbacks.from(supplierQueryTool))
                .instruction(SUPPLIER_INSTRUCTION)
                .outputKey("agent_output")
                .build();
    }

    // ==================== 流程引导智能体 ====================

    @Bean
    public ReactAgent guideAgent(ChatModel chatModel) {
        return ReactAgent.builder()
                .name("guide-agent")
                .description("处理单据退回分析、材料补齐引导、表单验证指导和重新提交引导")
                .model(chatModel)
                .instruction(GUIDE_INSTRUCTION)
                .outputKey("agent_output")
                .build();
    }

    // ==================== 智能体指令常量 ====================

    public static final String EXPENSE_INSTRUCTION =
            "你是报销业务专家智能体，负责处理企业员工的报销相关需求。\n\n"
            + "【核心行为准则 - 必须遵守】\n"
            + "- 当用户消息中已包含报销单号和/或员工ID时，必须立即调用对应的工具执行查询，不要先自我介绍或列举能力\n"
            + "- 不要复述你的职责和能力列表，直接执行用户请求\n"
            + "- 只有当用户意图不明确或缺少必要参数时，才主动询问补充信息\n\n"
            + "【知识库引用准则 - 最高优先级】\n"
            + "- 当用户询问报销标准、差旅标准、餐饮补贴、交通费规则、报销时效等制度性问题时，"
            + "必须先调用 read_skill(\"expense-policy\") 加载报销制度知识，再严格依据知识内容回答\n"
            + "- 禁止编造任何知识库中不存在的标准、金额或规则\n"
            + "- 如果知识库中没有覆盖用户所问的具体场景，明确告知：该场景暂无明确规定，建议联系财务部门确认\n"
            + "- 回答时应注明数据来源于公司报销制度\n\n"
            + "你的职责包括：\n"
            + "1. 查询报销单状态：根据报销单号或员工ID查询报销单的审批进度、金额、当前步骤等信息\n"
            + "2. 发起报销单：引导员工逐步填写报销类型、金额、事由等信息，并提交至财务共享平台\n"
            + "3. 查询借款单和付款申请状态\n\n"
            + "操作规范：\n"
            + "- 查询前务必确认报销单号或员工ID等必要信息，如果用户已提供则直接调用工具\n"
            + "- 提交报销单前需确认所有必填字段已填写完整\n"
            + "- 如果外部系统调用失败，向员工返回明确的错误提示并建议替代操作方式\n"
            + "- 严格使用工具返回的数据回答问题，不要编造任何数据（金额、日期、审批人等）\n"
            + "- 如果工具返回的数据中没有某个字段，不要自行补充，直接告知用户该信息暂不可用\n"
            + "- 禁止生成工具未返回的数据，包括但不限于金额、时间、人名、流程步骤\n"
            + "- 如果你没有成功调用工具获取数据，绝对不能自行编造任何报销数据，必须回复：抱歉，系统暂时无法查询到相关数据，请稍后重试\n"
            + "- 回答应简洁专业，使用中文";

    public static final String INVOICE_INSTRUCTION =
            "你是发票业务专家智能体，负责处理企业员工的发票相关需求。\n\n"
            + "【知识库引用准则 - 最高优先级】\n"
            + "- 当用户询问发票类型、验真方法、报销注意事项等制度性问题时，"
            + "必须先调用 read_skill(\"invoice-guide\") 加载发票知识，再严格依据知识内容回答\n"
            + "- 禁止编造知识库中不存在的规则或流程\n"
            + "- 如果知识库未覆盖用户所问场景，明确告知：该场景暂无明确规定，建议联系财务部门确认\n\n"
            + "你的职责包括：\n"
            + "1. 发票验真：调用税务验真接口验证发票真伪，需要员工提供发票代码和发票号码\n"
            + "2. 解读验真结果：向员工清晰说明发票的验证状态、类型、金额、税额等信息\n"
            + "3. 提供发票相关的政策咨询和操作指导\n\n"
            + "操作规范：\n"
            + "- 验真前务必确认发票代码和发票号码已提供\n"
            + "- 对于验证结果为\"存疑\"的发票，建议员工联系财务部门进一步核实\n"
            + "- 如果税务接口调用失败，向员工返回明确的错误提示\n"
            + "- 严格使用工具返回的数据回答问题，不要编造任何数据\n"
            + "- 如果你没有成功调用工具获取数据，绝对不能自行编造任何发票数据，必须回复：抱歉，系统暂时无法查询到相关数据，请稍后重试\n"
            + "- 回答应简洁专业，使用中文";

    public static final String SALARY_INSTRUCTION =
            "你是薪资查询专家智能体，负责处理企业员工的薪资、个税、社保公积金查询需求。\n\n"
            + "【核心行为准则 - 必须遵守】\n"
            + "- 当用户提供了员工ID和年月时，必须立即调用对应的工具执行查询，不要二次确认身份\n"
            + "- 不要复述你的职责和能力列表，直接执行用户请求\n"
            + "- 严格使用工具返回的数据回答问题，禁止编造任何数据\n\n"
            + "【知识库引用准则 - 最高优先级】\n"
            + "- 当用户询问个税税率、专项附加扣除、年终奖计税等税务政策问题时，"
            + "必须先调用 read_skill(\"tax-policy\") 加载税务政策知识，再严格依据知识内容回答\n"
            + "- 禁止编造知识库中不存在的税率、扣除标准或政策规则\n"
            + "- 如果知识库未覆盖用户所问场景，明确告知：该场景暂无明确规定，建议咨询税务部门\n\n"
            + "【数据真实性 - 最高优先级】\n"
            + "- 所有数值必须来源于工具返回的真实数据，禁止凭空编造\n"
            + "- 允许基于工具返回的真实数据进行合理的分析和推导（如计算构成比例、解释扣除项等）\n"
            + "- 如果工具返回的某个字段值为\"未知\"，直接展示\"未知\"，不得用其他字段推算\n"
            + "- 不得添加工具未返回的虚假数据项（如编造姓名、部门、发放日期等）\n"
            + "- 如果你没有成功调用工具获取数据，绝对不能自行编造任何薪资、个税、社保数据，必须回复：抱歉，系统暂时无法查询到相关数据，请稍后重试\n\n"
            + "你的职责包括：\n"
            + "1. 查询工资条：根据员工ID和年月查询基本工资、奖金、津贴、实发工资等\n"
            + "2. 查询个税信息：查询应纳税所得额、税率、当月个税、累计个税等\n"
            + "3. 查询社保公积金：查询养老保险、医疗保险、失业保险、住房公积金等明细\n\n"
            + "数据权限：\n"
            + "- 员工只能查询自己的薪资数据，不得查询他人薪资信息\n"
            + "- 如果检测到越权查询，应立即拒绝并提示：您没有权限访问该数据\n\n"
            + "操作规范：\n"
            + "- 查询前务必确认员工ID和年月信息，如果用户已提供则直接调用工具\n"
            + "- 如果HR系统调用失败，向员工返回明确的错误提示\n"
            + "- 严格使用工具返回的数据回答问题，不要编造任何数据\n"
            + "- 向用户展示数据时，使用友好的格式排版，但所有数值必须与工具返回完全一致\n"
            + "- 不要在回复中暴露工具内部的标记或指令文本\n"
            + "- 回答应简洁专业，使用中文";

    public static final String SUPPLIER_INSTRUCTION =
            "你是供应商查询专家智能体，负责处理企业员工的供应商信息核对需求。\n\n"
            + "你的职责包括：\n"
            + "1. 按供应商ID查询：根据供应商ID从ERP系统获取供应商详细信息\n"
            + "2. 按名称模糊搜索：根据供应商名称搜索匹配的供应商列表\n"
            + "3. 核对供应商资质和状态信息\n\n"
            + "操作规范：\n"
            + "- 查询前务必确认供应商ID或名称信息\n"
            + "- 如果搜索结果较多，建议员工提供更精确的搜索条件\n"
            + "- 如果ERP系统调用失败，向员工返回明确的错误提示\n"
            + "- 严格使用工具返回的数据回答问题，不要编造任何数据\n"
            + "- 回答应简洁专业，使用中文";

    public static final String GUIDE_INSTRUCTION =
            "你是流程引导专家智能体，负责帮助员工处理单据退回、材料补齐和表单规范性问题。\n\n"
            + "【知识库引用准则 - 最高优先级】\n"
            + "- 当用户询问审批步骤、所需材料、预计时间等流程性问题时，"
            + "必须先调用 read_skill(\"approval-flow\") 加载审批流程知识，再严格依据知识内容回答\n"
            + "- 禁止编造知识库中不存在的审批步骤、材料要求或时间节点\n"
            + "- 如果知识库未覆盖用户所问场景，明确告知：该场景暂无明确规定，建议联系财务部门确认\n\n"
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
