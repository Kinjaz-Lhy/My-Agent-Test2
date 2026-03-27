package com.company.finance.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查端点
 */
@RestController
public class HealthController {

    @GetMapping("/actuator/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("application", "finance-ai-customer-service");
        return Mono.just(result);
    }
}
