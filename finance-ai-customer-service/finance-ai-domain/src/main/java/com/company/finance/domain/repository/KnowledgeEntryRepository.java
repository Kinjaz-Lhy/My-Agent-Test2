package com.company.finance.domain.repository;

import com.company.finance.common.enums.KnowledgeStatus;
import com.company.finance.domain.entity.KnowledgeEntry;

import java.util.List;

/**
 * 知识条目仓储接口
 * <p>
 * 定义知识条目的持久化操作，由基础设施层实现。
 * </p>
 */
public interface KnowledgeEntryRepository {

    /**
     * 查询指定分类下的有效知识条目（status = ACTIVE）
     *
     * @param category 分类名称
     * @return 有效知识条目列表
     */
    List<KnowledgeEntry> findActiveByCategory(String category);

    /**
     * 更新知识条目状态
     *
     * @param entryId 条目 ID
     * @param status  目标状态
     */
    void updateStatus(String entryId, KnowledgeStatus status);

    /**
     * 保存知识条目
     *
     * @param entry 知识条目实体
     */
    void save(KnowledgeEntry entry);
}
