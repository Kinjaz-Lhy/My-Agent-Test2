package com.company.finance.api.security;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserPrincipal 单元测试。
 */
class UserPrincipalTest {

    @Test
    void shouldStoreEmployeeIdAndDepartmentId() {
        UserPrincipal principal = new UserPrincipal("EMP001", "DEPT-FIN",
                Collections.singletonList("ROLE_OPERATOR"));

        assertThat(principal.getEmployeeId()).isEqualTo("EMP001");
        assertThat(principal.getDepartmentId()).isEqualTo("DEPT-FIN");
    }

    @Test
    void shouldReturnUnmodifiableRoles() {
        UserPrincipal principal = new UserPrincipal("EMP001", "DEPT-FIN",
                Arrays.asList("ROLE_OPERATOR", "ROLE_AUDITOR"));

        assertThat(principal.getRoles()).containsExactly("ROLE_OPERATOR", "ROLE_AUDITOR");
    }

    @Test
    void shouldHandleNullRoles() {
        UserPrincipal principal = new UserPrincipal("EMP001", "DEPT-FIN", null);

        assertThat(principal.getRoles()).isEmpty();
    }

    @Test
    void hasRoleShouldMatchWithPrefix() {
        UserPrincipal principal = new UserPrincipal("EMP001", "DEPT-FIN",
                Collections.singletonList("ROLE_OPERATOR"));

        assertThat(principal.hasRole("OPERATOR")).isTrue();
        assertThat(principal.hasRole("ROLE_OPERATOR")).isTrue();
        assertThat(principal.hasRole("AUDITOR")).isFalse();
    }

    @Test
    void toStringShouldContainAllFields() {
        UserPrincipal principal = new UserPrincipal("EMP001", "DEPT-FIN",
                Collections.singletonList("ROLE_OPERATOR"));

        String str = principal.toString();
        assertThat(str).contains("EMP001");
        assertThat(str).contains("DEPT-FIN");
        assertThat(str).contains("ROLE_OPERATOR");
    }
}
