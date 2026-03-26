package com.company.finance.infrastructure.client;

import java.util.Map;

/**
 * 财务共享平台客户端接口
 * <p>
 * 对接财务共享服务平台（FSS），提供报销单查询、提交，
 * 借款单/付款申请查询等业务操作。
 * </p>
 */
public interface FSSPlatformClient {

    /**
     * 查询报销单状态
     *
     * @param expenseId  报销单号
     * @param employeeId 员工 ID
     * @return 报销单状态信息（包含状态、审批进度等）
     */
    Map<String, Object> queryExpenseStatus(String expenseId, String employeeId);

    /**
     * 提交报销单
     *
     * @param expenseData 报销单数据
     * @return 提交结果（包含单据号、提交状态等）
     */
    Map<String, Object> submitExpense(Map<String, Object> expenseData);

    /**
     * 查询借款单状态
     *
     * @param loanId     借款单号
     * @param employeeId 员工 ID
     * @return 借款单状态信息
     */
    Map<String, Object> queryLoanStatus(String loanId, String employeeId);

    /**
     * 提交开票申请
     *
     * @param billingData 开票申请数据
     * @return 提交结果
     */
    Map<String, Object> submitBilling(Map<String, Object> billingData);
}
