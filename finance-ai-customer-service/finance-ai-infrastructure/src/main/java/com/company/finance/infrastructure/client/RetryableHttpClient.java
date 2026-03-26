package com.company.finance.infrastructure.client;

import com.company.finance.common.exception.ExternalSystemTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 可重试 HTTP 客户端抽象基类
 * <p>
 * 封装外部系统调用的重试和超时熔断逻辑：
 * <ul>
 *   <li>最多重试 3 次</li>
 *   <li>重试间隔 2 秒</li>
 *   <li>超时时间 10 秒，超时后抛出 {@link ExternalSystemTimeoutException}</li>
 * </ul>
 * 子类需提供系统名称和 WebClient 实例。
 * </p>
 */
@Slf4j
public abstract class RetryableHttpClient {

    /** 最大重试次数 */
    protected static final int MAX_RETRIES = 3;

    /** 重试间隔（毫秒） */
    protected static final long RETRY_INTERVAL_MS = 2000L;

    /** 超时时间（毫秒） */
    protected static final long TIMEOUT_MS = 10000L;

    private final WebClient webClient;
    private final String systemName;

    protected RetryableHttpClient(WebClient webClient, String systemName) {
        this.webClient = webClient;
        this.systemName = systemName;
    }

    /**
     * 执行 GET 请求（带重试和超时熔断）
     *
     * @param uri 请求路径
     * @return 响应结果
     */
    protected Map<String, Object> doGet(String uri) {
        return executeWithRetry(() ->
                webClient.get()
                        .uri(uri)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofMillis(TIMEOUT_MS))
                        .onErrorMap(TimeoutException.class, e ->
                                new ExternalSystemTimeoutException(systemName, TIMEOUT_MS, e))
        );
    }

    /**
     * 执行 POST 请求（带重试和超时熔断）
     *
     * @param uri  请求路径
     * @param body 请求体
     * @return 响应结果
     */
    protected Map<String, Object> doPost(String uri, Object body) {
        return executeWithRetry(() ->
                webClient.post()
                        .uri(uri)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofMillis(TIMEOUT_MS))
                        .onErrorMap(TimeoutException.class, e ->
                                new ExternalSystemTimeoutException(systemName, TIMEOUT_MS, e))
        );
    }

    /**
     * 带重试机制的请求执行
     * <p>
     * 最多重试 {@link #MAX_RETRIES} 次，每次间隔 {@link #RETRY_INTERVAL_MS} 毫秒。
     * 如果所有重试均失败，抛出最后一次异常。
     * 超时异常不进行重试，直接抛出。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> executeWithRetry(MonoSupplier monoSupplier) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.debug("[{}] 第 {} 次调用外部系统", systemName, attempt);
                Map<String, Object> result = (Map<String, Object>) monoSupplier.get().block();
                return result;
            } catch (ExternalSystemTimeoutException e) {
                // 超时异常直接抛出，不重试
                log.error("[{}] 调用超时，不再重试", systemName, e);
                throw e;
            } catch (Exception e) {
                lastException = e;
                log.warn("[{}] 第 {} 次调用失败: {}", systemName, attempt, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试等待被中断", ie);
                    }
                }
            }
        }

        log.error("[{}] 已达最大重试次数 {}，调用失败", systemName, MAX_RETRIES);
        throw new RuntimeException(
                String.format("外部系统 [%s] 调用失败，已重试 %d 次", systemName, MAX_RETRIES),
                lastException);
    }

    /**
     * 获取系统名称
     */
    protected String getSystemName() {
        return systemName;
    }

    /**
     * 获取 WebClient 实例
     */
    protected WebClient getWebClient() {
        return webClient;
    }

    /**
     * Mono 供应者函数式接口（兼容 JDK 8）
     */
    @FunctionalInterface
    protected interface MonoSupplier {
        Mono<?> get();
    }
}
