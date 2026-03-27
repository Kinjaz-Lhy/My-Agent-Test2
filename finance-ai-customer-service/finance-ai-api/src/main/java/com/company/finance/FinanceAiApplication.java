package com.company.finance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 财务共享智能 AI 客服系统启动类
 * <p>
 * 基于 Spring Boot + Spring WebFlux 构建，
 * 集成 MyBatis 数据访问、Spring Security 安全认证和 AI-Nova 智能体框架。
 * </p>
 *
 * @see <a href="需求 7.2, 7.6">WebFlux 响应式编程 &amp; 多模块架构</a>
 */
@SpringBootApplication(scanBasePackages = "com.company.finance")
@MapperScan("com.company.finance.infrastructure.mapper")
public class FinanceAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceAiApplication.class, args);
    }
}
