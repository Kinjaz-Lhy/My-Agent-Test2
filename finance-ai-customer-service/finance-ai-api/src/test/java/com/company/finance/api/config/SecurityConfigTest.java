package com.company.finance.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityConfig 单元测试 — 验证 JWT 认证转换器的角色提取和 UserPrincipal 解析逻辑。
 */
class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void jwtConverterShouldExtractRolesFromRolesClaim() {
        Jwt jwt = buildJwt("EMP001", "DEPT-FIN", Arrays.asList("OPERATOR", "AUDITOR"), null);

        Converter<Jwt, Mono<AbstractAuthenticationToken>> converter = securityConfig.jwtAuthenticationConverter();
        AbstractAuthenticationToken token = converter.convert(jwt).block();

        assertThat(token).isNotNull();
        Collection<GrantedAuthority> authorities = token.getAuthorities();
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_OPERATOR", "ROLE_AUDITOR");
    }

    @Test
    void jwtConverterShouldExtractScopeAuthorities() {
        Jwt jwt = buildJwt("EMP001", "DEPT-FIN", Collections.emptyList(), "read write");

        Converter<Jwt, Mono<AbstractAuthenticationToken>> converter = securityConfig.jwtAuthenticationConverter();
        AbstractAuthenticationToken token = converter.convert(jwt).block();

        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("SCOPE_read", "SCOPE_write");
    }

    @Test
    void jwtConverterShouldUseEmployeeIdClaimAsName() {
        Jwt jwt = buildJwt("EMP001", "DEPT-FIN", Collections.singletonList("OPERATOR"), null);

        Converter<Jwt, Mono<AbstractAuthenticationToken>> converter = securityConfig.jwtAuthenticationConverter();
        AbstractAuthenticationToken token = converter.convert(jwt).block();

        assertThat(token).isNotNull();
        assertThat(token.getName()).isEqualTo("EMP001");
    }

    @Test
    void jwtConverterShouldFallbackToSubjectWhenNoEmployeeId() {
        // 构建不含 employee_id claim 的 JWT
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user-sub-123");
        claims.put("roles", Collections.singletonList("OPERATOR"));

        Jwt jwt = new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600),
                headers, claims);

        Converter<Jwt, Mono<AbstractAuthenticationToken>> converter = securityConfig.jwtAuthenticationConverter();
        AbstractAuthenticationToken token = converter.convert(jwt).block();

        assertThat(token).isNotNull();
        assertThat(token.getName()).isEqualTo("user-sub-123");
    }

    @Test
    void jwtConverterShouldHandleRolesWithRolePrefix() {
        // 角色已带 ROLE_ 前缀，不应重复添加
        Jwt jwt = buildJwt("EMP001", "DEPT-FIN", Arrays.asList("ROLE_OPERATOR", "AUDITOR"), null);

        Converter<Jwt, Mono<AbstractAuthenticationToken>> converter = securityConfig.jwtAuthenticationConverter();
        AbstractAuthenticationToken token = converter.convert(jwt).block();

        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_OPERATOR", "ROLE_AUDITOR");
    }

    @Test
    void jwtConverterShouldHandleEmptyRoles() {
        Jwt jwt = buildJwt("EMP001", "DEPT-FIN", Collections.emptyList(), null);

        Converter<Jwt, Mono<AbstractAuthenticationToken>> converter = securityConfig.jwtAuthenticationConverter();
        AbstractAuthenticationToken token = converter.convert(jwt).block();

        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).isEmpty();
    }

    /**
     * 构建测试用 JWT。
     */
    private Jwt buildJwt(String employeeId, String departmentId, Collection<String> roles, String scope) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", employeeId);
        claims.put("employee_id", employeeId);
        claims.put("department_id", departmentId);
        claims.put("roles", roles);
        if (scope != null) {
            claims.put("scope", scope);
        }

        return new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600),
                headers, claims);
    }
}
