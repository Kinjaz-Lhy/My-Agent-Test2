package com.company.finance.service.knowledge;

import com.company.finance.common.enums.KnowledgeStatus;
import com.company.finance.domain.entity.KnowledgeEntry;
import com.company.finance.infrastructure.mapper.KnowledgeEntryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库查询与管理服务
 * <p>
 * 提供按分类检索、报销标准匹配、审批流程查询、税务政策查询等能力。
 * 当知识库无匹配结果时，返回降级提示信息。
 * </p>
 *
 * @see <a href="需求 2.1">财务制度知识检索</a>
 * @see <a href="需求 2.2">报销标准按职级城市匹配</a>
 * @see <a href="需求 2.3">审批流程查询</a>
 * @see <a href="需求 2.4">税务政策查询</a>
 * @see <a href="需求 2.5">知识库无匹配降级提示</a>
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    /** 降级提示信息：知识库无匹配时返回 */
    static final String FALLBACK_MESSAGE = "暂未找到相关信息，建议联系人工客服";

    /** 报销制度分类名称 */
    static final String CATEGORY_EXPENSE = "报销制度";

    /** 审批流程分类名称 */
    static final String CATEGORY_APPROVAL = "审批流程";

    /** 税务政策分类名称 */
    static final String CATEGORY_TAX = "税务政策";

    private final KnowledgeEntryMapper knowledgeEntryMapper;

    public KnowledgeService(KnowledgeEntryMapper knowledgeEntryMapper) {
        this.knowledgeEntryMapper = knowledgeEntryMapper;
    }

    /**
     * 按分类检索有效知识条目（status = ACTIVE）。
     *
     * @param category 分类名称（如"报销制度"、"税务政策"、"审批流程"）
     * @return 有效知识条目列表，分类为空时返回空列表
     */
    public List<KnowledgeEntry> findActiveByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            log.warn("知识库查询分类为空");
            return Collections.emptyList();
        }
        List<KnowledgeEntry> entries = knowledgeEntryMapper.selectActiveByCategory(category);
        log.debug("按分类[{}]检索到 {} 条有效知识条目", category, entries.size());
        return entries;
    }

    /**
     * 报销标准按职级和城市匹配查询。
     * <p>
     * 从"报销制度"分类的有效条目中，筛选标题或内容同时包含指定职级和城市的条目。
     * </p>
     *
     * @param level 员工职级（如"P5"、"M1"）
     * @param city  所在城市（如"北京"、"上海"）
     * @return 匹配的报销标准条目列表
     */
    public List<KnowledgeEntry> findExpenseStandard(String level, String city) {
        if (level == null || level.trim().isEmpty() || city == null || city.trim().isEmpty()) {
            log.warn("报销标准查询参数不完整：level={}, city={}", level, city);
            return Collections.emptyList();
        }

        List<KnowledgeEntry> allExpenseEntries = findActiveByCategory(CATEGORY_EXPENSE);
        List<KnowledgeEntry> matched = allExpenseEntries.stream()
                .filter(entry -> matchesLevelAndCity(entry, level, city))
                .collect(Collectors.toList());

        log.debug("报销标准查询 level={}, city={}, 匹配 {} 条", level, city, matched.size());
        return matched;
    }

    /**
     * 审批流程查询，返回包含步骤、材料、预计时间的结果。
     *
     * @param flowType 流程类型（如"差旅报销"、"借款申请"）
     * @return 审批流程结果，无匹配时返回 null
     */
    public ApprovalFlowResult findApprovalFlow(String flowType) {
        if (flowType == null || flowType.trim().isEmpty()) {
            log.warn("审批流程查询类型为空");
            return null;
        }

        List<KnowledgeEntry> approvalEntries = findActiveByCategory(CATEGORY_APPROVAL);
        KnowledgeEntry matched = approvalEntries.stream()
                .filter(entry -> entry.getTitle() != null && entry.getTitle().contains(flowType))
                .findFirst()
                .orElse(null);

        if (matched == null) {
            log.debug("未找到流程类型[{}]的审批流程", flowType);
            return null;
        }

        return parseApprovalFlow(matched);
    }

    /**
     * 税务政策查询，仅返回 ACTIVE 且 effectiveAt <= 当前时间的条目。
     *
     * @return 当前有效的税务政策条目列表
     */
    public List<KnowledgeEntry> findActiveTaxPolicies() {
        List<KnowledgeEntry> taxEntries = findActiveByCategory(CATEGORY_TAX);
        LocalDateTime now = LocalDateTime.now();

        List<KnowledgeEntry> effective = taxEntries.stream()
                .filter(entry -> entry.getEffectiveAt() != null && !entry.getEffectiveAt().isAfter(now))
                .collect(Collectors.toList());

        log.debug("税务政策查询：共 {} 条 ACTIVE，其中 {} 条已生效", taxEntries.size(), effective.size());
        return effective;
    }

    /**
     * 带降级提示的知识库查询。
     * <p>
     * 按分类检索有效知识条目，若无匹配结果则返回包含降级提示的结果。
     * </p>
     *
     * @param category 分类名称
     * @return 查询结果，包含条目列表和可能的降级提示
     */
    public FallbackResult findWithFallback(String category) {
        List<KnowledgeEntry> entries = findActiveByCategory(category);
        if (entries.isEmpty()) {
            log.info("知识库分类[{}]无匹配结果，返回降级提示", category);
            return new FallbackResult(Collections.emptyList(), FALLBACK_MESSAGE);
        }
        return new FallbackResult(entries, null);
    }

    // ========== 知识库更新审核流程 ==========

    /**
     * 提交知识条目进行审核。
     * <p>
     * 将条目状态设为 PENDING_REVIEW 后插入数据库。
     * 未审核的条目不会出现在 {@link #findActiveByCategory} 的查询结果中。
     * </p>
     *
     * @param entry 待提交的知识条目
     * @return 插入影响行数
     * @see <a href="需求 2.6">知识库更新审核生效</a>
     * @see <a href="需求 6.5">知识库更新待审核状态</a>
     */
    public int submitForReview(KnowledgeEntry entry) {
        if (entry == null) {
            log.warn("提交审核的知识条目为空");
            return 0;
        }
        entry.setStatus(KnowledgeStatus.PENDING_REVIEW);
        int rows = knowledgeEntryMapper.insert(entry);
        log.info("知识条目[{}]已提交审核，状态置为 PENDING_REVIEW", entry.getEntryId());
        return rows;
    }

    /**
     * 审核通过知识条目，状态变为 ACTIVE。
     *
     * @param entryId    知识条目 ID
     * @param reviewedBy 审核人
     * @return 更新影响行数
     * @see <a href="需求 6.5">知识库更新待审核状态</a>
     */
    public int approveEntry(String entryId, String reviewedBy) {
        if (entryId == null || entryId.trim().isEmpty()) {
            log.warn("审核通过操作缺少 entryId");
            return 0;
        }
        int rows = knowledgeEntryMapper.updateStatus(entryId, KnowledgeStatus.ACTIVE.name());
        log.info("知识条目[{}]审核通过，审核人：{}，状态变为 ACTIVE", entryId, reviewedBy);
        return rows;
    }

    /**
     * 审核驳回知识条目，状态变为 DRAFT。
     *
     * @param entryId    知识条目 ID
     * @param reviewedBy 审核人
     * @return 更新影响行数
     * @see <a href="需求 6.5">知识库更新待审核状态</a>
     */
    public int rejectEntry(String entryId, String reviewedBy) {
        if (entryId == null || entryId.trim().isEmpty()) {
            log.warn("审核驳回操作缺少 entryId");
            return 0;
        }
        int rows = knowledgeEntryMapper.updateStatus(entryId, KnowledgeStatus.DRAFT.name());
        log.info("知识条目[{}]审核驳回，审核人：{}，状态变为 DRAFT", entryId, reviewedBy);
        return rows;
    }

    // ========== 内部方法 ==========

    /**
     * 判断知识条目的标题或内容是否同时包含指定职级和城市。
     */
    private boolean matchesLevelAndCity(KnowledgeEntry entry, String level, String city) {
        String title = entry.getTitle() != null ? entry.getTitle() : "";
        String content = entry.getContent() != null ? entry.getContent() : "";
        String combined = title + content;
        return combined.contains(level) && combined.contains(city);
    }

    /**
     * 从知识条目内容中解析审批流程信息。
     * <p>
     * 内容格式约定：
     * <pre>
     * 步骤：1.提交申请;2.部门审批;3.财务审核
     * 材料：发票原件;费用明细表;审批单
     * 预计时间：3个工作日
     * </pre>
     * </p>
     */
    ApprovalFlowResult parseApprovalFlow(KnowledgeEntry entry) {
        String content = entry.getContent();
        if (content == null || content.trim().isEmpty()) {
            return new ApprovalFlowResult(
                    Collections.singletonList(entry.getTitle()),
                    Collections.emptyList(),
                    "未知"
            );
        }

        List<String> steps = new ArrayList<>();
        List<String> materials = new ArrayList<>();
        String estimatedDuration = "未知";

        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("步骤：") || trimmed.startsWith("步骤:")) {
                String value = trimmed.substring(3).trim();
                for (String step : value.split(";")) {
                    String s = step.trim();
                    if (!s.isEmpty()) {
                        steps.add(s);
                    }
                }
            } else if (trimmed.startsWith("材料：") || trimmed.startsWith("材料:")) {
                String value = trimmed.substring(3).trim();
                for (String material : value.split(";")) {
                    String m = material.trim();
                    if (!m.isEmpty()) {
                        materials.add(m);
                    }
                }
            } else if (trimmed.startsWith("预计时间：") || trimmed.startsWith("预计时间:")) {
                estimatedDuration = trimmed.substring(5).trim();
            }
        }

        // 如果解析不到步骤，将标题作为默认步骤
        if (steps.isEmpty()) {
            steps.add(entry.getTitle());
        }

        return new ApprovalFlowResult(steps, materials, estimatedDuration);
    }

    // ========== 内部 DTO ==========

    /**
     * 审批流程查询结果
     */
    public static class ApprovalFlowResult {

        /** 审批步骤列表 */
        private final List<String> steps;

        /** 所需材料列表 */
        private final List<String> materials;

        /** 预计时间 */
        private final String estimatedDuration;

        public ApprovalFlowResult(List<String> steps, List<String> materials, String estimatedDuration) {
            this.steps = steps != null ? steps : Collections.emptyList();
            this.materials = materials != null ? materials : Collections.emptyList();
            this.estimatedDuration = estimatedDuration != null ? estimatedDuration : "未知";
        }

        public List<String> getSteps() {
            return steps;
        }

        public List<String> getMaterials() {
            return materials;
        }

        public String getEstimatedDuration() {
            return estimatedDuration;
        }

        @Override
        public String toString() {
            return "ApprovalFlowResult{" +
                    "steps=" + steps +
                    ", materials=" + materials +
                    ", estimatedDuration='" + estimatedDuration + '\'' +
                    '}';
        }
    }

    /**
     * 带降级提示的查询结果
     */
    public static class FallbackResult {

        /** 匹配的知识条目列表 */
        private final List<KnowledgeEntry> entries;

        /** 降级提示信息（有匹配结果时为 null） */
        private final String fallbackMessage;

        public FallbackResult(List<KnowledgeEntry> entries, String fallbackMessage) {
            this.entries = entries != null ? entries : Collections.emptyList();
            this.fallbackMessage = fallbackMessage;
        }

        public List<KnowledgeEntry> getEntries() {
            return entries;
        }

        public String getFallbackMessage() {
            return fallbackMessage;
        }

        /** 是否触发了降级 */
        public boolean isFallback() {
            return fallbackMessage != null;
        }

        @Override
        public String toString() {
            return "FallbackResult{" +
                    "entries=" + entries.size() +
                    ", fallbackMessage='" + fallbackMessage + '\'' +
                    '}';
        }
    }
}
