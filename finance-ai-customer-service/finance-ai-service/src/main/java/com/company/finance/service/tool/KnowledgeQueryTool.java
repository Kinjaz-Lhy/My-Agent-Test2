package com.company.finance.service.tool;

import com.company.finance.domain.entity.KnowledgeEntry;
import com.company.finance.service.knowledge.KnowledgeService;

import kd.ai.nova.core.tool.annotation.Tool;
import kd.ai.nova.core.tool.annotation.ToolParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库查询工具。
 * <p>
 * 从 t_knowledge_entry 表查询运营人员维护的知识条目。
 * 分类不硬编码，支持按分类编码或关键词模糊匹配。
 * </p>
 */
@Component
public class KnowledgeQueryTool {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeQueryTool.class);

    private final KnowledgeService knowledgeService;

    public KnowledgeQueryTool(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @Tool(description = "从企业知识库查询信息。当用户询问企业内部系统、平台功能、业务流程、制度规范等问题时必须调用此工具。"
            + "参数 category 可传分类编码或关键词，传空字符串则查询全部。")
    public String queryKnowledge(
            @ToolParam(description = "知识分类编码或关键词，传空字符串查询全部") String category) {
        log.info("知识库查询工具被调用: category='{}'", category);

        // 1. 先按分类精确查询（ACTIVE 状态）
        List<KnowledgeEntry> entries = null;
        if (category != null && !category.trim().isEmpty()) {
            entries = knowledgeService.findActiveByCategory(category.trim());
        }

        // 2. 精确分类没查到，按关键词在全部条目中模糊匹配
        if (entries == null || entries.isEmpty()) {
            List<KnowledgeEntry> all = knowledgeService.findByCategory(null);
            if (all != null && category != null && !category.trim().isEmpty()) {
                String keyword = category.trim().toLowerCase();
                entries = all.stream()
                        .filter(e -> isActive(e) && matchesKeyword(e, keyword))
                        .collect(Collectors.toList());
            } else if (all != null) {
                entries = all.stream().filter(this::isActive).collect(Collectors.toList());
            }
        }

        if (entries == null || entries.isEmpty()) {
            return "知识库中未找到与「" + category + "」相关的有效条目。";
        }

        String result = entries.stream()
                .map(e -> "【" + e.getTitle() + "】\n" + e.getContent())
                .collect(Collectors.joining("\n\n"));

        log.info("知识库查询返回 {} 条结果, category='{}'", entries.size(), category);
        return result;
    }

    private boolean isActive(KnowledgeEntry e) {
        return e.getStatus() != null && "ACTIVE".equals(e.getStatus().name());
    }

    private boolean matchesKeyword(KnowledgeEntry e, String keyword) {
        return contains(e.getTitle(), keyword)
                || contains(e.getContent(), keyword)
                || contains(e.getCategory(), keyword);
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword);
    }
}
