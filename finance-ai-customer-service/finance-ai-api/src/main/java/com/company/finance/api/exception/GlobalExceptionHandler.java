package com.company.finance.api.exception;

import com.company.finance.common.dto.ErrorResponse;
import com.company.finance.common.exception.ExternalSystemTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;

import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获各类异常并返回标准化的 {@link ErrorResponse}，
 * 确保 API 层不暴露内部异常堆栈信息。
 * </p>
 *
 * @see <a href="需求 3.8, 5.6, 7.5">外部系统错误处理、API 授权校验、超时熔断</a>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理外部系统超时异常 → 503 Service Unavailable
     */
    @ExceptionHandler(ExternalSystemTimeoutException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTimeout(ExternalSystemTimeoutException e) {
        log.warn("外部系统超时: {}", e.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("TIMEOUT", "系统繁忙，请稍后重试")));
    }

    /**
     * 处理权限不足异常 → 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN", "您没有权限访问该数据")));
    }

    /**
     * 处理身份认证异常 → 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAuthentication(AuthenticationException e) {
        log.warn("身份认证失败: {}", e.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("UNAUTHORIZED", "身份验证失败，请重新登录")));
    }

    /**
     * 处理 WebFlux 请求体校验异常 → 400 Bad Request
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(WebExchangeBindException e) {
        String details = e.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", details);
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", details)));
    }

    /**
     * 处理非法参数异常 → 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", e.getMessage())));
    }

    /**
     * 处理所有未捕获的异常 → 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception e) {
        log.error("系统内部错误", e);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "系统内部错误")));
    }
}
