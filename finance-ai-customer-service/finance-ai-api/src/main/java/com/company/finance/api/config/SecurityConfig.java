package com.company.finance.api.config;

import com.company.finance.api.security.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security WebFlux 安全配置。
 * <p>
 * 配置 OAuth2 Resource Server JWT 验证（SSO 集成），
 * 以及基于路径的权限规则：
 * <ul>
 *   <li>/api/v1/admin/** → 需要 OPERATOR 角色</li>
 *   <li>/api/v1/chat/** → 需要已认证用户</li>
 *   <li>/api/v1/audit/** → 需要 AUDITOR 角色</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * 配置安全过滤链。
     */
    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                // 禁用 CSRF（REST API 使用 JWT，无需 CSRF 保护）
                .csrf().disable()
                // 配置 OAuth2 Resource Server JWT 验证
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                // 配置路径权限规则
                .authorizeExchange(exchanges -> exchanges
                        // 公开端点：健康检查、Actuator、静态资源
                        .pathMatchers("/actuator/**", "/actuator/health/**").permitAll()
                        .pathMatchers("/favicon.ico", "/error").permitAll()
                        // 业务端点权限
                        .pathMatchers("/api/v1/admin/**").hasRole("OPERATOR")
                        .pathMatchers("/api/v1/chat/**").authenticated()
                        .pathMatchers("/api/v1/audit/**").hasRole("AUDITOR")
                        .anyExchange().authenticated()
                )
                .build();
    }

    /**
     * JWT 认证转换器：从 JWT Token 中提取用户信息和角色，
     * 构建包含 UserPrincipal 的认证令牌。
     */
    Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        return jwt -> {
            // 从 JWT claims 中提取角色列表
            Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

            // 从 JWT claims 中解析 UserPrincipal
            UserPrincipal principal = extractUserPrincipal(jwt);

            JwtAuthenticationToken authToken = new JwtAuthenticationToken(jwt, authorities, principal.getEmployeeId());
            return Mono.just(authToken);
        };
    }

    /**
     * 从 JWT 中提取权限列表。
     * <p>
     * 支持两种 claim 格式：
     * <ul>
     *   <li>"roles" claim：角色名称列表（自动添加 ROLE_ 前缀）</li>
     *   <li>"scope" claim：空格分隔的权限字符串</li>
     * </ul>
     * </p>
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 从 "roles" claim 提取角色
        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim instanceof Collection) {
            Collection<String> roles = (Collection<String>) rolesClaim;
            for (String role : roles) {
                // 确保角色带有 ROLE_ 前缀（Spring Security hasRole() 要求）
                String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                authorities.add(new SimpleGrantedAuthority(authority));
            }
        }

        // 从 "scope" claim 提取权限
        Object scopeClaim = jwt.getClaim("scope");
        if (scopeClaim instanceof String) {
            String[] scopes = ((String) scopeClaim).split("\\s+");
            for (String scope : scopes) {
                if (!scope.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
                }
            }
        }

        return authorities;
    }

    /**
     * 从 JWT 中解析 UserPrincipal。
     * <p>
     * 提取 employeeId（sub claim）、departmentId 和 roles。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private UserPrincipal extractUserPrincipal(Jwt jwt) {
        // 员工 ID：优先使用 "employee_id" claim，回退到 "sub"
        String employeeId = jwt.getClaimAsString("employee_id");
        if (employeeId == null || employeeId.isEmpty()) {
            employeeId = jwt.getSubject();
        }

        // 部门 ID
        String departmentId = jwt.getClaimAsString("department_id");

        // 角色列表
        List<String> roles;
        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim instanceof Collection) {
            roles = ((Collection<String>) rolesClaim).stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .collect(Collectors.toList());
        } else {
            roles = Collections.emptyList();
        }

        return new UserPrincipal(employeeId, departmentId, roles);
    }
}
