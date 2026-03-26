package com.company.finance.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 满意度评价实体
 * <p>
 * 会话结束时收集的员工满意度评价，
 * 包含 1-5 分评分和可选的文字反馈。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SatisfactionFeedback {

    /** 评价唯一标识 */
    private String feedbackId;

    /** 会话 ID */
    private String sessionId;

    /** 员工 ID */
    private String employeeId;

    /** 评分（1-5） */
    private int score;

    /** 文字反馈（可选） */
    private String comment;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 基于 feedbackId 判断相等性，支持序列化往返比较
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SatisfactionFeedback that = (SatisfactionFeedback) o;
        return feedbackId != null && feedbackId.equals(that.feedbackId);
    }

    @Override
    public int hashCode() {
        return feedbackId != null ? feedbackId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SatisfactionFeedback{" +
                "feedbackId='" + feedbackId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", score=" + score +
                ", createdAt=" + createdAt +
                '}';
    }
}
