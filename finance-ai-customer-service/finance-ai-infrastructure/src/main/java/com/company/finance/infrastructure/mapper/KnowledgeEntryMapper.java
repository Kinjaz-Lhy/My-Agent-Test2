package com.company.finance.infrastructure.mapper;

import com.company.finance.domain.entity.KnowledgeEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识条目 Mapper 接口
 * <p>
 * 提供知识条目的插入、状态更新和按分类查询操作。
 * </p>
 */
@Mapper
public interface KnowledgeEntryMapper {

    /**
     * 查询指定分类下状态为 ACTIVE 的知识条目
     *
     * @param category 分类名称
     * @return 有效知识条目列表
     */
    List<KnowledgeEntry> selectActiveByCategory(@Param("category") String category);

    /**
     * 更新知识条目状态
     *
     * @param entryId 知识条目 ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatus(@Param("entryId") String entryId, @Param("status") String status);

    /**
     * 插入知识条目
     *
     * @param entry 知识条目实体
     * @return 影响行数
     */
    int insert(KnowledgeEntry entry);
}
