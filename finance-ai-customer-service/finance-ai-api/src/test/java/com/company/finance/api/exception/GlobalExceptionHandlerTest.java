package com.company.finance.api.exception;

import com.company.finance.common.dto.ErrorResponse;
import com.company.finance.common.exception.ExternalSystemTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 单元测试
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleTimeoutShouldReturn503WithTimeoutCode() {
        ExternalSystemTimeoutException ex = new ExternalSystemTimeoutException("ERP", 10000);

        Mono<ResponseEntity<ErrorResponse>> result = handler.handleTimeout(ex);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    ErrorResponse body = response.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.getCode()).isEqualTo("TIMEOUT");
                    assertThat(body.getMessage()).isEqualTo("系统繁忙，请稍后重试");
                })
                .verifyComplete();
    }

    @Test
    void handleAccessDeniedShouldReturn403WithForbiddenCode() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        Mono<ResponseEntity<ErrorResponse>> result = handler.handleAccessDenied(ex);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    ErrorResponse body = response.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.getCode()).isEqualTo("FORBIDDEN");
                    assertThat(body.getMessage()).isEqualTo("您没有权限访问该数据");
                })
                .verifyComplete();
    }

    @Test
    void handleAuthenticationShouldReturn401WithUnauthorizedCode() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        Mono<ResponseEntity<ErrorResponse>> result = handler.handleAuthentication(ex);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    ErrorResponse body = response.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.getCode()).isEqualTo("UNAUTHORIZED");
                    assertThat(body.getMessage()).isEqualTo("身份验证失败，请重新登录");
                })
                .verifyComplete();
    }

    @Test
    void handleValidationShouldReturn400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("request", "message", "不能为空");
        FieldError fieldError2 = new FieldError("request", "sessionId", "格式不正确");
        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        WebExchangeBindException ex = mock(WebExchangeBindException.class);
        when(ex.getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        Mono<ResponseEntity<ErrorResponse>> result = handler.handleValidation(ex);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    ErrorResponse body = response.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(body.getMessage()).contains("message: 不能为空");
                    assertThat(body.getMessage()).contains("sessionId: 格式不正确");
                })
                .verifyComplete();
    }

    @Test
    void handleIllegalArgumentShouldReturn400WithMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("无效的会话ID");

        Mono<ResponseEntity<ErrorResponse>> result = handler.handleIllegalArgument(ex);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    ErrorResponse body = response.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.getCode()).isEqualTo("BAD_REQUEST");
                    assertThat(body.getMessage()).isEqualTo("无效的会话ID");
                })
                .verifyComplete();
    }

    @Test
    void handleGenericExceptionShouldReturn500WithInternalErrorCode() {
        RuntimeException ex = new RuntimeException("Unexpected error");

        Mono<ResponseEntity<ErrorResponse>> result = handler.handleGenericException(ex);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    ErrorResponse body = response.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.getCode()).isEqualTo("INTERNAL_ERROR");
                    assertThat(body.getMessage()).isEqualTo("系统内部错误");
                })
                .verifyComplete();
    }

    @Test
    void handleGenericExceptionShouldNotExposeStackTrace() {
        RuntimeException ex = new RuntimeException("NullPointerException at com.company.finance.internal.SomeClass.method");

        Mono<ResponseEntity<ErrorResponse>> result = handler.handleGenericException(ex);

        StepVerifier.create(result)
                .assertNext(response -> {
                    ErrorResponse body = response.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.getMessage()).doesNotContain("NullPointerException");
                    assertThat(body.getMessage()).doesNotContain("com.company");
                    assertThat(body.getMessage()).isEqualTo("系统内部错误");
                })
                .verifyComplete();
    }
}
