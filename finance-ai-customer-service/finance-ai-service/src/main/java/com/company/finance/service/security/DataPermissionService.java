package com.company.finance.service.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据权限校验服务。
 * <p>
 * 根据员工身份和部门校验数据访问权限，仅返回有权访问的数据。
 * 权限规则：
 * <ul>
 *   <li>普通员工：只能访问自己的数据</li>
 *   <li>部门经理：可以访问同部门下属的数据（简化实现：同部门即可访问）</li>
 *   <li>管理员：可以访问所有数据</li>
 * </ul>
 * </p>
 *
 * @see <a href="需求 5.2">根据员工身份和部门执行数据权限校验</a>
 */
@Service
public class DataPermissionService {

    private static final Logger log = LoggerFactory.getLogger(DataPermissionService.class);

    /** 管理员角色标识 */
    static final String ROLE_ADMIN = "ROLE_ADMIN";

    /** 部门经理角色标识 */
    static final String ROLE_MANAGER = "ROLE_MANAGER";

    /**
     * 检查请求者是否有权访问目标员工的数据。
     * <p>
     * 权限判断逻辑：
     * <ol>
     *   <li>管理员可以访问所有数据</li>
     *   <li>员工可以访问自己的数据</li>
     *   <li>部门经理可以访问同部门员工的数据</li>
     * </ol>
     * </p>
     *
     * @param requestingEmployeeId   请求者员工 ID
     * @param requestingDepartmentId 请求者部门 ID
     * @param targetEmployeeId       目标员工 ID（被访问数据的所属员工）
     * @param roles                  请求者的角色列表
     * @return 如果有权访问返回 true，否则返回 false
     */
    public boolean hasPermission(String requestingEmployeeId,
                                 String requestingDepartmentId,
                                 String targetEmployeeId,
                                 List<String> roles) {
        // 管理员可以访问所有数据
        if (roles != null && roles.contains(ROLE_ADMIN)) {
            return true;
        }

        // 员工可以访问自己的数据
        if (requestingEmployeeId != null && requestingEmployeeId.equals(targetEmployeeId)) {
            return true;
        }

        // 部门经理可以访问同部门员工的数据（需要目标员工部门信息，简化实现见重载方法）
        // 此方法无法判断目标员工部门，仅支持自身和管理员场景
        return false;
    }

    /**
     * 检查请求者是否有权访问目标员工的数据（含目标员工部门信息）。
     * <p>
     * 权限判断逻辑：
     * <ol>
     *   <li>管理员可以访问所有数据</li>
     *   <li>员工可以访问自己的数据</li>
     *   <li>部门经理可以访问同部门员工的数据</li>
     * </ol>
     * </p>
     *
     * @param requestingEmployeeId   请求者员工 ID
     * @param requestingDepartmentId 请求者部门 ID
     * @param targetEmployeeId       目标员工 ID
     * @param targetDepartmentId     目标员工部门 ID
     * @param roles                  请求者的角色列表
     * @return 如果有权访问返回 true，否则返回 false
     */
    public boolean hasPermission(String requestingEmployeeId,
                                 String requestingDepartmentId,
                                 String targetEmployeeId,
                                 String targetDepartmentId,
                                 List<String> roles) {
        // 管理员可以访问所有数据
        if (roles != null && roles.contains(ROLE_ADMIN)) {
            return true;
        }

        // 员工可以访问自己的数据
        if (requestingEmployeeId != null && requestingEmployeeId.equals(targetEmployeeId)) {
            return true;
        }

        // 部门经理可以访问同部门员工的数据
        if (roles != null && roles.contains(ROLE_MANAGER)
                && requestingDepartmentId != null
                && requestingDepartmentId.equals(targetDepartmentId)) {
            return true;
        }

        log.debug("数据权限校验拒绝：请求者={}, 部门={}, 目标员工={}, 目标部门={}",
                requestingEmployeeId, requestingDepartmentId, targetEmployeeId, targetDepartmentId);
        return false;
    }

    /**
     * 按权限过滤数据列表，仅保留请求者有权访问的数据。
     * <p>
     * 通过 employeeIdExtractor 从每条数据中提取所属员工 ID，
     * 然后逐条校验权限，过滤掉无权访问的数据。
     * </p>
     *
     * @param employeeId          请求者员工 ID
     * @param departmentId        请求者部门 ID
     * @param roles               请求者的角色列表
     * @param dataList            待过滤的数据列表
     * @param employeeIdExtractor 从数据对象中提取所属员工 ID 的函数
     * @param <T>                 数据类型
     * @return 过滤后的数据列表，仅包含有权访问的数据
     */
    public <T> List<T> filterByPermission(String employeeId,
                                          String departmentId,
                                          List<String> roles,
                                          List<T> dataList,
                                          Function<T, String> employeeIdExtractor) {
        if (dataList == null || dataList.isEmpty()) {
            return Collections.emptyList();
        }

        // 管理员可以访问所有数据，直接返回
        if (roles != null && roles.contains(ROLE_ADMIN)) {
            return dataList;
        }

        return dataList.stream()
                .filter(item -> {
                    String targetEmployeeId = employeeIdExtractor.apply(item);
                    return hasPermission(employeeId, departmentId, targetEmployeeId, roles);
                })
                .collect(Collectors.toList());
    }

    /**
     * 按权限过滤数据列表（含目标部门信息提取）。
     *
     * @param employeeId              请求者员工 ID
     * @param departmentId            请求者部门 ID
     * @param roles                   请求者的角色列表
     * @param dataList                待过滤的数据列表
     * @param employeeIdExtractor     从数据对象中提取所属员工 ID 的函数
     * @param departmentIdExtractor   从数据对象中提取所属部门 ID 的函数
     * @param <T>                     数据类型
     * @return 过滤后的数据列表，仅包含有权访问的数据
     */
    public <T> List<T> filterByPermission(String employeeId,
                                          String departmentId,
                                          List<String> roles,
                                          List<T> dataList,
                                          Function<T, String> employeeIdExtractor,
                                          Function<T, String> departmentIdExtractor) {
        if (dataList == null || dataList.isEmpty()) {
            return Collections.emptyList();
        }

        // 管理员可以访问所有数据，直接返回
        if (roles != null && roles.contains(ROLE_ADMIN)) {
            return dataList;
        }

        return dataList.stream()
                .filter(item -> {
                    String targetEmployeeId = employeeIdExtractor.apply(item);
                    String targetDepartmentId = departmentIdExtractor.apply(item);
                    return hasPermission(employeeId, departmentId, targetEmployeeId, targetDepartmentId, roles);
                })
                .collect(Collectors.toList());
    }
}
