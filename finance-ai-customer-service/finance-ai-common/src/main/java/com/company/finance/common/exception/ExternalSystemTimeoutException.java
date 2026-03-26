package com.company.finance.common.exception;

/**
 * 外部系统调用超时异常
 * <p>
 * 当外部系统（财务共享平台、ERP、税务接口、HR 系统等）
 * 调用超过 10 秒未响应时抛出此异常。
 * </p>
 */
public class ExternalSystemTimeoutException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 超时的外部系统名称 */
    private final String systemName;

    /** 超时时间（毫秒） */
    private final long timeoutMs;

    public ExternalSystemTimeoutException(String systemName, long timeoutMs) {
        super(String.format("外部系统 [%s] 调用超时，超时时间: %d ms", systemName, timeoutMs));
        this.systemName = systemName;
        this.timeoutMs = timeoutMs;
    }

    public ExternalSystemTimeoutException(String systemName, long timeoutMs, Throwable cause) {
        super(String.format("外部系统 [%s] 调用超时，超时时间: %d ms", systemName, timeoutMs), cause);
        this.systemName = systemName;
        this.timeoutMs = timeoutMs;
    }

    public String getSystemName() {
        return systemName;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }
}
