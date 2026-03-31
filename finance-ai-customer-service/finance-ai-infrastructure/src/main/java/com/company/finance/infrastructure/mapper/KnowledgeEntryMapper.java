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
     * 查询指定分类下所有知识条目（不限状态），分类为空时查全部
     *
     * @param category 分类名称（可选）
     * @return 知识条目列表
     */
    List<KnowledgeEntry> selectByCategory(@Param("category") String category);

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

    /**
     * 删除知识条目
     */
    int deleteById(@Param("entryId") String entryId);

    /**
     * 更新知识条目内容
     */
    int updateEntry(KnowledgeEntry entry);

    /**
     * 查询所有不重复的分类
     */
    List<String> selectDistinctCategories();
}
