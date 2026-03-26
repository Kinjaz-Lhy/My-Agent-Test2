package com.company.finance.infrastructure.client;

import java.util.Map;

/**
 * HR 系统客户端接口
 * <p>
 * 对接企业 HR 系统，提供薪资、个税、社保公积金等信息查询。
 * </p>
 */
public interface HRClient {

    /**
     * 查询薪资信息
     *
     * @param employeeId 员工 ID
     * @param yearMonth  年月（格式：yyyy-MM）
     * @return 薪资详情
     */
    Map<String, Object> querySalary(String employeeId, String yearMonth);

    /**
     * 查询个税信息
     *
     * @param employeeId 员工 ID
     * @param yearMonth  年月（格式：yyyy-MM）
     * @return 个税详情
     */
    Map<String, Object> queryTax(String employeeId, String yearMonth);

    /**
     * 查询社保公积金信息
     *
     * @param employeeId 员工 ID
     * @param yearMonth  年月（格式：yyyy-MM）
     * @return 社保公积金详情
     */
    Map<String, Object> querySocialInsurance(String employeeId, String yearMonth);
}
