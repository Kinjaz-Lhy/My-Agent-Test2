package com.company.finance.domain.entity;

import com.company.finance.common.enums.KnowledgeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识条目实体
 * <p>
 * 存储企业财务制度、报销标准、税务政策、审批流程等知识信息。
 * 支持审核流程：DRAFT → PENDING_REVIEW → ACTIVE / ARCHIVED。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeEntry {

    /** 知识条目唯一标识 */
    private String entryId;

    /** 分类：报销制度 / 税务政策 / 审批流程 等 */
    private String category;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 状态：DRAFT / PENDING_REVIEW / ACTIVE / ARCHIVED */
    private KnowledgeStatus status;

    /** 创建人 */
    private String createdBy;

    /** 审核人 */
    private String reviewedBy;

    /** 生效时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveAt;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 基于 entryId 判断相等性，支持序列化往返比较
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KnowledgeEntry that = (KnowledgeEntry) o;
        return entryId != null && entryId.equals(that.entryId);
    }

    @Override
    public int hashCode() {
        return entryId != null ? entryId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "KnowledgeEntry{" +
                "entryId='" + entryId + '\'' +
                ", category='" + category + '\'' +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
