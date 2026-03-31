package com.company.finance.service.knowledge;

import com.company.finance.domain.entity.KnowledgeCategory;
import com.company.finance.infrastructure.mapper.KnowledgeCategoryMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeCategoryService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeCategoryService.class);

    private final KnowledgeCategoryMapper categoryMapper;

    public KnowledgeCategoryService(KnowledgeCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<KnowledgeCategory> findAll() {
        List<KnowledgeCategory> list = categoryMapper.selectAll();
        return list != null ? list : Collections.emptyList();
    }

    public KnowledgeCategory findById(String categoryId) {
        return categoryMapper.selectById(categoryId);
    }

    public KnowledgeCategory create(KnowledgeCategory category) {
        if (category.getCategoryId() == null || category.getCategoryId().isEmpty()) {
            category.setCategoryId(UUID.randomUUID().toString());
        }
        categoryMapper.insert(category);
        log.info("新增知识分类: code={}, name={}", category.getCode(), category.getName());
        return category;
    }

    public int update(KnowledgeCategory category) {
        int rows = categoryMapper.update(category);
        log.info("更新知识分类: categoryId={}, code={}, name={}", category.getCategoryId(), category.getCode(), category.getName());
        return rows;
    }

    public int delete(String categoryId) {
        int rows = categoryMapper.deleteById(categoryId);
        log.info("删除知识分类: categoryId={}", categoryId);
        return rows;
    }
}
