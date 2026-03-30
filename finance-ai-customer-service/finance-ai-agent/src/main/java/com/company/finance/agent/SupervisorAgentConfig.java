package com.company.finance.agent;

import kd.ai.nova.graph.CompileConfig;
import kd.ai.nova.graph.agent.Agent;
import kd.ai.nova.graph.agent.ReactAgent;
import kd.ai.nova.graph.agent.flow.agent.SupervisorAgent;
import kd.ai.nova.core.model.chat.ChatModel;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * SupervisorAgent 顶层调度器配置。
 * <p>
 * 基于 AI-Nova SupervisorAgent 实现 LLM 动态调度，
 * 根据用户意图自动选择合适的业务 ReactAgent。
 * </p>
 *
 * @see <a href="需求 3.1, 3.3, 3.5, 3.7">智能体调度正确性</a>
 */
@Configuration
public class SupervisorAgentConfig {

    static final String SUPERVISOR_INSTRUCTION =
            "你是企业财务共享中心的智能客服调度器，负责根据用户意图将请求分配给合适的业务智能体。\n\n"
            + "【核心行为准则 - 必须遵守】\n"
            + "- 收到用户消息后，立即判断意图并调度对应的子智能体，不要先自我介绍或列举能力\n"
            + "- 不要复述你的调度规则或可用智能体列表，直接执行调度\n"
            + "- 只有当用户意图确实无法判断时，才简短询问以明确需求\n\n"
            + "可用的业务智能体：\n"
            + "1. expense-agent（报销智能体）：处理报销单查询、报销单发起、借款单/付款申请查询\n"
            + "2. invoice-agent（发票智能体）：处理发票验真、发票相关咨询\n"
            + "3. salary-agent（薪资智能体）：处理工资条查询、个税查询、社保公积金查询\n"
            + "4. supplier-agent（供应商智能体）：处理供应商信息核对、供应商搜索\n"
            + "5. guide-agent（流程引导智能体）：处理单据退回分析、材料补齐引导、表单验证指导\n\n"
            + "调度规则：\n"
            + "- 涉及报销、借款、付款相关的查询或办理 → expense-agent\n"
            + "- 涉及发票验真、发票查询 → invoice-agent\n"
            + "- 涉及工资、薪资、个税、社保、公积金 → salary-agent\n"
            + "- 涉及供应商信息核对、供应商查询 → supplier-agent\n"
            + "- 涉及单据退回、材料补齐、表单填写规范 → guide-agent\n"
            + "- 如果是打招呼、闲聊或一般性问题（如'你好'、'在吗'），不要调度任何智能体，直接友好回复即可\n"
            + "- 如果用户意图不明确，请先询问用户以明确需求，不要调度智能体\n"
            + "- 如果无法匹配任何智能体，告知用户并建议转接人工客服\n\n"
            + "注意事项：\n"
            + "- 每次只调度一个最合适的智能体\n"
            + "- 当子智能体返回结果后，直接将结果回复给用户，不要再次调度\n"
            + "- 如果子智能体已经给出了完整回答，任务即完成，无需继续调度\n"
            + "- 使用中文与用户交流";

    /**
     * 创建 SupervisorAgent 顶层调度器。
     * <p>
     * 将所有业务 ReactAgent 注册为子智能体，
     * 由 LLM 根据用户意图动态选择调度目标。
     * </p>
     */
    @Bean
    public SupervisorAgent financeSupervisor(
            ChatModel chatModel,
            @Qualifier("expenseAgent") ReactAgent expenseAgent,
            @Qualifier("invoiceAgent") ReactAgent invoiceAgent,
            @Qualifier("salaryAgent") ReactAgent salaryAgent,
            @Qualifier("supplierAgent") ReactAgent supplierAgent,
            @Qualifier("guideAgent") ReactAgent guideAgent) {

        return SupervisorAgent.builder()
                .name("finance-supervisor")
                .model(chatModel)
                .subAgents(Arrays.<Agent>asList(expenseAgent, invoiceAgent, salaryAgent, supplierAgent, guideAgent))
                .instruction(SUPERVISOR_INSTRUCTION)
                .compileConfig(CompileConfig.builder().recursionLimit(10).build())
                .build();
    }
}
