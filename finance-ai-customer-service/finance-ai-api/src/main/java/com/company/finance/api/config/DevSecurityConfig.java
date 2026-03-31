package com.company.finance.api.config;

import com.company.finance.api.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dev 环境安全配置：放行所有端点，但从 mock JWT 中解析用户身份。
 * 仅在 spring.profiles.active=dev 时生效。
 */
@Configuration
@EnableWebFluxSecurity
@Profile("dev")
public class DevSecurityConfig {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Bean
    @Primary
    public SecurityWebFilterChain devSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .httpBasic().disable()
                .formLogin().disable()
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                .addFilterBefore(devJwtParsingFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    /**
     * Dev 环境 WebFilter：从 Authorization header 中解析 mock JWT，
     * 提取用户身份放入 SecurityContext，实现按用户隔离。
     */
    private WebFilter devJwtParsingFilter() {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    String[] parts = token.split("\\.");
                    if (parts.length >= 2) {
                        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                        Map<String, Object> claims = MAPPER.readValue(payload, Map.class);

                        String employeeId = (String) claims.getOrDefault("sub", "dev-user");
                        String departmentId = (String) claims.getOrDefault("departmentId", "DEPT-DEV");
                        List<String> roles;
                        Object rolesClaim = claims.get("roles");
                        if (rolesClaim instanceof List) {
                            roles = ((List<?>) rolesClaim).stream()
                                    .map(r -> r.toString().startsWith("ROLE_") ? r.toString() : "ROLE_" + r)
                                    .collect(Collectors.toList());
                        } else {
                            roles = Collections.singletonList("ROLE_EMPLOYEE");
                        }

                        UserPrincipal principal = new UserPrincipal(employeeId, departmentId, roles);
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                principal, null,
                                roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));

                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                    }
                } catch (Exception ignored) {
                    // 解析失败，继续走默认逻辑
                }
            }
            return chain.filter(exchange);
        };
    }
}
