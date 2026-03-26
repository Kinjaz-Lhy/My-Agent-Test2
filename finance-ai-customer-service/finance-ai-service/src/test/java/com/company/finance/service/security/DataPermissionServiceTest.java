package com.company.finance.service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataPermissionService 单元测试
 * <p>
 * 验证数据权限校验逻辑：普通员工只能访问自己的数据，
 * 部门经理可以访问同部门数据，管理员可以访问所有数据。
 * </p>
 */
class DataPermissionServiceTest {

    private DataPermissionService service;

    @BeforeEach
    void setUp() {
        service = new DataPermissionService();
    }

    // ========== hasPermission（不含目标部门）==========

    @Nested
    @DisplayName("hasPermission - 基础权限校验")
    class HasPermissionBasic {

        @Test
        @DisplayName("管理员可以访问任意员工数据")
        void adminCanAccessAnyData() {
            List<String> roles = Collections.singletonList("ROLE_ADMIN");
            assertThat(service.hasPermission("EMP001", "DEPT01", "EMP999", roles)).isTrue();
        }

        @Test
        @DisplayName("员工可以访问自己的数据")
        void employeeCanAccessOwnData() {
            List<String> roles = Collections.emptyList();
            assertThat(service.hasPermission("EMP001", "DEPT01", "EMP001", roles)).isTrue();
        }

        @Test
        @DisplayName("普通员工不能访问其他员工数据")
        void employeeCannotAccessOtherData() {
            List<String> roles = Collections.emptyList();
            assertThat(service.hasPermission("EMP001", "DEPT01", "EMP002", roles)).isFalse();
        }

        @Test
        @DisplayName("角色列表为 null 时，员工仍可访问自己的数据")
        void nullRolesStillAllowsSelfAccess() {
            assertThat(service.hasPermission("EMP001", "DEPT01", "EMP001", (List<String>) null)).isTrue();
        }

        @Test
        @DisplayName("角色列表为 null 时，不能访问他人数据")
        void nullRolesCannotAccessOtherData() {
            assertThat(service.hasPermission("EMP001", "DEPT01", "EMP002", (List<String>) null)).isFalse();
        }
    }

    // ========== hasPermission（含目标部门）==========

    @Nested
    @DisplayName("hasPermission - 含部门信息的权限校验")
    class HasPermissionWithDepartment {

        @Test
        @DisplayName("管理员可以访问任意部门任意员工数据")
        void adminCanAccessAnyDepartmentData() {
            List<String> roles = Collections.singletonList("ROLE_ADMIN");
            assertThat(service.hasPermission("EMP001", "DEPT01", "EMP999", "DEPT99", roles)).isTrue();
        }

        @Test
        @DisplayName("员工可以访问自己的数据（跨部门场景）")
        void employeeCanAccessOwnDataAcrossDepartment() {
            List<String> roles = Collections.emptyList();
            assertThat(service.hasPermission("EMP001", "DEPT01", "EMP001", "DEPT02", roles)).isTrue();
        }

        @Test
        @DisplayName("部门经理可以访问同部门员工数据")
        void managerCanAccessSameDepartmentData() {
            List<String> roles = Collections.singletonList("ROLE_MANAGER");
            assertThat(service.hasPermission("EMP001", "DEPT01", "EMP002", "DEPT01", roles)).isTrue();
        }

        @Test
        @DisplayName("部门经理不能访问其他部门员工数据")
        void managerCannotAccessOtherDepartmentData() {
            List<String> roles = Collections.singletonList("ROLE_MANAGER");
            assertThat(service.hasPermission("EMP001", "DEPT01", "EMP002", "DEPT02", roles)).isFalse();
        }

        @Test
        @DisplayName("普通员工不能访问同部门其他员工数据")
        void regularEmployeeCannotAccessSameDeptOtherData() {
            List<String> roles = Collections.emptyList();
            assertThat(service.hasPermission("EMP001", "DEPT01", "EMP002", "DEPT01", roles)).isFalse();
        }

        @Test
        @DisplayName("部门经理同时拥有管理员角色时可以访问所有数据")
        void managerWithAdminCanAccessAll() {
            List<String> roles = Arrays.asList("ROLE_MANAGER", "ROLE_ADMIN");
            assertThat(service.hasPermission("EMP001", "DEPT01", "EMP999", "DEPT99", roles)).isTrue();
        }

        @Test
        @DisplayName("请求者部门 ID 为 null 时，经理无法访问他人数据")
        void nullRequestingDeptDeniesManagerAccess() {
            List<String> roles = Collections.singletonList("ROLE_MANAGER");
            assertThat(service.hasPermission("EMP001", null, "EMP002", "DEPT01", roles)).isFalse();
        }
    }

    // ========== filterByPermission ==========

    @Nested
    @DisplayName("filterByPermission - 数据列表过滤")
    class FilterByPermission {

        @Test
        @DisplayName("null 列表返回空列表")
        void nullListReturnsEmpty() {
            List<String> result = service.filterByPermission(
                    "EMP001", "DEPT01", Collections.emptyList(),
                    null, s -> s);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("空列表返回空列表")
        void emptyListReturnsEmpty() {
            List<String> result = service.filterByPermission(
                    "EMP001", "DEPT01", Collections.emptyList(),
                    Collections.emptyList(), s -> s);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("管理员获取全部数据")
        void adminGetsAllData() {
            List<String> roles = Collections.singletonList("ROLE_ADMIN");
            List<String> data = Arrays.asList("EMP001", "EMP002", "EMP003");

            List<String> result = service.filterByPermission(
                    "EMP001", "DEPT01", roles, data, s -> s);
            assertThat(result).containsExactly("EMP001", "EMP002", "EMP003");
        }

        @Test
        @DisplayName("普通员工只能获取自己的数据")
        void regularEmployeeGetsOnlyOwnData() {
            List<String> roles = Collections.emptyList();
            List<String> data = Arrays.asList("EMP001", "EMP002", "EMP003");

            List<String> result = service.filterByPermission(
                    "EMP001", "DEPT01", roles, data, s -> s);
            assertThat(result).containsExactly("EMP001");
        }

        @Test
        @DisplayName("列表中无自己的数据时返回空列表")
        void noOwnDataReturnsEmpty() {
            List<String> roles = Collections.emptyList();
            List<String> data = Arrays.asList("EMP002", "EMP003");

            List<String> result = service.filterByPermission(
                    "EMP001", "DEPT01", roles, data, s -> s);
            assertThat(result).isEmpty();
        }
    }

    // ========== filterByPermission（含部门提取器）==========

    @Nested
    @DisplayName("filterByPermission - 含部门信息的数据列表过滤")
    class FilterByPermissionWithDepartment {

        /** 简单的数据记录，包含员工 ID 和部门 ID */
        class DataRecord {
            final String employeeId;
            final String departmentId;

            DataRecord(String employeeId, String departmentId) {
                this.employeeId = employeeId;
                this.departmentId = departmentId;
            }
        }

        @Test
        @DisplayName("部门经理获取同部门所有员工数据")
        void managerGetsSameDepartmentData() {
            List<String> roles = Collections.singletonList("ROLE_MANAGER");
            List<DataRecord> data = Arrays.asList(
                    new DataRecord("EMP001", "DEPT01"),
                    new DataRecord("EMP002", "DEPT01"),
                    new DataRecord("EMP003", "DEPT02")
            );

            List<DataRecord> result = service.filterByPermission(
                    "EMP001", "DEPT01", roles, data,
                    r -> r.employeeId, r -> r.departmentId);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(r -> r.employeeId)
                    .containsExactly("EMP001", "EMP002");
        }

        @Test
        @DisplayName("管理员获取所有部门所有数据")
        void adminGetsAllDepartmentData() {
            List<String> roles = Collections.singletonList("ROLE_ADMIN");
            List<DataRecord> data = Arrays.asList(
                    new DataRecord("EMP001", "DEPT01"),
                    new DataRecord("EMP002", "DEPT02"),
                    new DataRecord("EMP003", "DEPT03")
            );

            List<DataRecord> result = service.filterByPermission(
                    "EMP001", "DEPT01", roles, data,
                    r -> r.employeeId, r -> r.departmentId);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("普通员工只获取自己的数据，忽略同部门他人数据")
        void regularEmployeeOnlyGetsOwnData() {
            List<String> roles = Collections.emptyList();
            List<DataRecord> data = Arrays.asList(
                    new DataRecord("EMP001", "DEPT01"),
                    new DataRecord("EMP002", "DEPT01"),
                    new DataRecord("EMP003", "DEPT02")
            );

            List<DataRecord> result = service.filterByPermission(
                    "EMP001", "DEPT01", roles, data,
                    r -> r.employeeId, r -> r.departmentId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).employeeId).isEqualTo("EMP001");
        }

        @Test
        @DisplayName("null 列表返回空列表")
        void nullListReturnsEmpty() {
            List<DataRecord> result = service.filterByPermission(
                    "EMP001", "DEPT01", Collections.emptyList(),
                    null, r -> r.employeeId, r -> r.departmentId);
            assertThat(result).isEmpty();
        }
    }
}
