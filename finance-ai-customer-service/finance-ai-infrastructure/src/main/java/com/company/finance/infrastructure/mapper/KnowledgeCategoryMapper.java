package com.company.finance.infrastructure.mapper;

import com.company.finance.domain.entity.KnowledgeCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeCategoryMapper {

    List<KnowledgeCategory> selectAll();

    KnowledgeCategory selectById(@Param("categoryId") String categoryId);

    int insert(KnowledgeCategory category);

    int update(KnowledgeCategory category);

    int deleteById(@Param("categoryId") String categoryId);
}
