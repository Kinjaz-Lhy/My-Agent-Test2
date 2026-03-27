package com.company.finance.api.config;

import com.company.finance.api.security.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dev 环境安全配置：放行所有端点，无需 JWT。
 * 仅在 spring.profiles.active=dev 时生效。
 */
@Configuration
@EnableWebFluxSecurity
@Profile("dev")
public class DevSecurityConfig {

    @Bean
    @Primary
    public SecurityWebFilterChain devSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .httpBasic().disable()
                .formLogin().disable()
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                .build();
    }
}
