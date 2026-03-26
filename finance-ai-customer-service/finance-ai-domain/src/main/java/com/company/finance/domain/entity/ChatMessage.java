package com.company.finance.domain.entity;

import com.company.finance.common.enums.MessageRole;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 对话消息实体
 * <p>
 * 表示会话中的一条消息，包含角色、内容、意图和元数据。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    /** 消息唯一标识 */
    private String messageId;

    /** 所属会话 ID */
    private String sessionId;

    /** 消息角色：USER / ASSISTANT / SYSTEM / TOOL */
    private MessageRole role;

    /** 消息内容 */
    private String content;

    /** 识别的意图（可选） */
    private String intent;

    /** 元数据（工具调用结果等） */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /** 消息时间戳 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 基于 messageId 判断相等性，支持序列化往返比较
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return messageId != null && messageId.equals(that.messageId);
    }

    @Override
    public int hashCode() {
        return messageId != null ? messageId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "messageId='" + messageId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", role=" + role +
                ", content='" + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
                ", intent='" + intent + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
