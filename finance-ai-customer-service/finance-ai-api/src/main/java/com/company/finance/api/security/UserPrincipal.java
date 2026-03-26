package com.company.finance.api.security;

import java.util.Collections;
import java.util.List;

/**
 * 用户主体信息，从 JWT Token 中解析。
 * <p>
 * 包含员工 ID、部门 ID 和角色列表，用于权限校验和业务逻辑中的身份识别。
 * </p>
 */
public class UserPrincipal {

    /** 员工 ID */
    private final String employeeId;

    /** 部门 ID */
    private final String departmentId;

    /** 角色列表（如 ROLE_OPERATOR、ROLE_AUDITOR） */
    private final List<String> roles;

    public UserPrincipal(String employeeId, String departmentId, List<String> roles) {
        this.employeeId = employeeId;
        this.departmentId = departmentId;
        this.roles = roles != null ? Collections.unmodifiableList(roles) : Collections.emptyList();
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public List<String> getRoles() {
        return roles;
    }

    /**
     * 判断用户是否拥有指定角色。
     *
     * @param role 角色名称（不含 ROLE_ 前缀）
     * @return 是否拥有该角色
     */
    public boolean hasRole(String role) {
        String prefixed = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return roles.contains(prefixed);
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "employeeId='" + employeeId + '\'' +
                ", departmentId='" + departmentId + '\'' +
                ", roles=" + roles +
                '}';
    }
}
