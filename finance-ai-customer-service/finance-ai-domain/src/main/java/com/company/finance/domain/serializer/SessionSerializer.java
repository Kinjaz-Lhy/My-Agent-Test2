package com.company.finance.domain.serializer;

import com.company.finance.domain.entity.Session;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 会话序列化器
 * <p>
 * 基于 Jackson ObjectMapper 实现 Session 对象与 JSON 字符串的互转，
 * 支持 LocalDateTime、枚举、Map 等复杂类型的序列化配置。
 * </p>
 */
@Slf4j
public class SessionSerializer {

    /** 标准 ObjectMapper（紧凑输出） */
    private final ObjectMapper objectMapper;

    /** 格式化 ObjectMapper（缩进输出） */
    private final ObjectMapper prettyMapper;

    public SessionSerializer() {
        this.objectMapper = createObjectMapper(false);
        this.prettyMapper = createObjectMapper(true);
    }

    /**
     * 创建并配置 ObjectMapper
     *
     * @param prettyPrint 是否启用缩进格式化输出
     * @return 配置好的 ObjectMapper 实例
     */
    private ObjectMapper createObjectMapper(boolean prettyPrint) {
        ObjectMapper mapper = new ObjectMapper();

        // 注册 JavaTimeModule 支持 LocalDateTime 序列化
        mapper.registerModule(new JavaTimeModule());

        // 禁用日期时间戳格式，使用 ISO-8601 字符串格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 枚举序列化为字符串（默认行为，显式确认）
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        // 反序列化时忽略未知属性，增强兼容性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 启用缩进输出
        if (prettyPrint) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }

        return mapper;
    }

    /**
     * 将 Session 对象序列化为 JSON 字符串
     *
     * @param session 会话对象
     * @return JSON 字符串
     * @throws SessionSerializationException 序列化失败时抛出
     */
    public String serialize(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("待序列化的 Session 对象不能为 null");
        }
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            log.error("Session 序列化失败, sessionId={}", session.getSessionId(), e);
            throw new SessionSerializationException("Session 序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为 Session 对象
     *
     * @param json JSON 字符串
     * @return Session 对象
     * @throws SessionSerializationException 反序列化失败时抛出
     */
    public Session deserialize(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("待反序列化的 JSON 字符串不能为空");
        }
        try {
            return objectMapper.readValue(json, Session.class);
        } catch (IOException e) {
            log.error("Session 反序列化失败, json={}", json.length() > 200 ? json.substring(0, 200) + "..." : json, e);
            throw new SessionSerializationException("Session 反序列化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 Session 对象格式化输出为缩进的 JSON 字符串
     *
     * @param session 会话对象
     * @return 格式化的 JSON 字符串
     * @throws SessionSerializationException 序列化失败时抛出
     */
    public String prettyPrint(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("待格式化的 Session 对象不能为 null");
        }
        try {
            return prettyMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            log.error("Session 格式化输出失败, sessionId={}", session.getSessionId(), e);
            throw new SessionSerializationException("Session 格式化输出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 会话序列化异常
     */
    public static class SessionSerializationException extends RuntimeException {

        public SessionSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
