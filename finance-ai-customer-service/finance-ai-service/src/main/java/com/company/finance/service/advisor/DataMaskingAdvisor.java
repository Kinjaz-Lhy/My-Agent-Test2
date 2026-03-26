package com.company.finance.service.advisor;

import com.company.finance.service.advisor.DataMaskingService;
import kd.ai.nova.chat.advisor.api.CallAdvisor;
import kd.ai.nova.chat.advisor.api.CallAdvisorChain;
import kd.ai.nova.chat.ChatClientRequest;
import kd.ai.nova.chat.ChatClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 敏感信息脱敏 Advisor
 * <p>
 * 实现 AI-Nova CallAdvisor 接口，在 AI 响应返回后对响应内容执行脱敏处理。
 * 脱敏逻辑委托给 {@link DataMaskingService}，覆盖身份证号、银行卡号、工资金额等敏感信息。
 * </p>
 * <p>
 * 集成方式：在 ChatClient Advisor 链中注册本 Advisor，
 * 确保在响应返回给用户之前完成脱敏处理。
 * </p>
 *
 * @see DataMaskingService
 */
public class DataMaskingAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(DataMaskingAdvisor.class);

    /** Advisor 名称 */
    private static final String ADVISOR_NAME = "DataMaskingAdvisor";

    /** 执行顺序，数值较大表示靠后执行（在响应返回前最后处理脱敏） */
    private static final int DEFAULT_ORDER = 100;

    private final DataMaskingService dataMaskingService;

    /**
     * 使用默认的 DataMaskingService 实例
     */
    public DataMaskingAdvisor() {
        this.dataMaskingService = new DataMaskingService();
    }

    /**
     * 使用指定的 DataMaskingService 实例（便于测试注入）
     *
     * @param dataMaskingService 脱敏服务实例
     */
    public DataMaskingAdvisor(DataMaskingService dataMaskingService) {
        this.dataMaskingService = dataMaskingService;
    }

    @Override
    public String getName() {
        return ADVISOR_NAME;
    }

    @Override
    public int getOrder() {
        return DEFAULT_ORDER;
    }

    /**
     * 在 Advisor 链中拦截同步调用，对 AI 响应内容执行脱敏处理。
     * <p>
     * 先将请求传递给下游 Advisor 链获取响应，
     * 然后对响应文本执行敏感信息掩码替换。
     * </p>
     *
     * @param request 聊天请求
     * @param chain   Advisor 调用链
     * @return 脱敏后的响应
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 先调用下游 Advisor 链获取原始响应
        ChatClientResponse response = chain.adviseCall(request);

        // 对响应内容执行脱敏处理
        try {
            String originalContent = response.getResult().getOutput().getContent();
            if (originalContent != null && !originalContent.isEmpty()) {
                String maskedContent = dataMaskingService.mask(originalContent);
                if (!maskedContent.equals(originalContent)) {
                    log.debug("敏感信息脱敏处理完成，原始长度={}，脱敏后长度={}",
                            originalContent.length(), maskedContent.length());
                }
                response.getResult().getOutput().setContent(maskedContent);
            }
        } catch (Exception e) {
            // 脱敏失败不应阻断正常响应流程，仅记录警告日志
            log.warn("敏感信息脱敏处理异常，返回原始响应", e);
        }

        return response;
    }
}
