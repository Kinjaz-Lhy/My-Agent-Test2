package com.company.finance.common.constants;

/**
 * 错误码常量
 * <p>
 * 定义系统中所有标准化错误码，
 * 与 {@link com.company.finance.common.dto.ErrorResponse} 配合使用。
 * </p>
 */
public final class ErrorCode {

    private ErrorCode() {
        // 工具类禁止实例化
    }

    // ==================== 通用错误 ====================

    /** 请求超时 */
    public static final String TIMEOUT = "TIMEOUT";

    /** 无权访问 */
    public static final String FORBIDDEN = "FORBIDDEN";

    /** 未认证 */
    public static final String UNAUTHORIZED = "UNAUTHORIZED";

    /** 参数校验失败 */
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    /** 服务内部错误 */
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    // ==================== 业务错误 ====================

    /** 会话不存在 */
    public static final String SESSION_NOT_FOUND = "SESSION_NOT_FOUND";

    /** 会话已关闭 */
    public static final String SESSION_CLOSED = "SESSION_CLOSED";

    /** 外部系统调用失败 */
    public static final String EXTERNAL_SYSTEM_ERROR = "EXTERNAL_SYSTEM_ERROR";

    /** 知识库无匹配结果 */
    public static final String KNOWLEDGE_NOT_FOUND = "KNOWLEDGE_NOT_FOUND";

    /** 账户已锁定 */
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";

    /** 人工转接中 */
    public static final String HUMAN_HANDOFF = "HUMAN_HANDOFF";
}
