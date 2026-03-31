package com.company.finance.domain.entity;

import com.company.finance.common.enums.SessionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话实体
 * <p>
 * 表示员工与智能客服系统之间的一次完整对话交互，
 * 包含多轮消息、上下文元数据和会话状态。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Session {

    /** 会话唯一标识 */
    private String sessionId;

    /** 员工 ID */
    private String employeeId;

    /** 部门 ID */
    private String departmentId;

    /** 会话状态：ACTIVE / TRANSFERRED / CLOSED */
    private SessionStatus status;

    /** 会话标题（用户自定义） */
    private String title;

    /** 是否置顶 */
    @Builder.Default
    private Boolean pinned = false;

    /** 置顶时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pinnedAt;

    /** 消息列表 */
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    /** 上下文元数据 */
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /** 关闭时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closedAt;

    /**
     * 基于 sessionId 判断相等性，支持序列化往返比较
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return sessionId != null && sessionId.equals(session.sessionId);
    }

    @Override
    public int hashCode() {
        return sessionId != null ? sessionId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", departmentId='" + departmentId + '\'' +
                ", status=" + status +
                ", messagesCount=" + (messages != null ? messages.size() : 0) +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", closedAt=" + closedAt +
                '}';
    }
}
