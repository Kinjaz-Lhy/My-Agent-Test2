package com.company.finance.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * WebFlux CORS 配置
 * <p>
 * 允许前端开发服务器（localhost:3000）跨域访问后端 API，
 * 支持 SSE（text/event-stream）流式响应的跨域传输。
 * </p>
 *
 * @see <a href="需求 7.2, 9.2">WebFlux 响应式编程、SSE 流式输出</a>
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许前端开发服务器来源
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));

        // 允许的 HTTP 方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允许的请求头（含 Authorization 用于 JWT）
        config.setAllowedHeaders(Arrays.asList(
                "Content-Type", "Authorization", "Accept", "Cache-Control"
        ));

        // 允许携带凭证（Cookie / Authorization header）
        config.setAllowCredentials(true);

        // 暴露响应头（SSE 需要 Content-Type 可见）
        config.setExposedHeaders(Arrays.asList("Content-Type", "Cache-Control"));

        // 预检请求缓存时间（1 小时）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsWebFilter(source);
    }
}
