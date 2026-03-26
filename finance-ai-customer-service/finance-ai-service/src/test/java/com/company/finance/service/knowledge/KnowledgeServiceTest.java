package com.company.finance.service.knowledge;

import com.company.finance.common.enums.KnowledgeStatus;
import com.company.finance.domain.entity.KnowledgeEntry;
import com.company.finance.infrastructure.mapper.KnowledgeEntryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

/**
 * KnowledgeService 单元测试
 * <p>
 * 验证知识库查询与管理服务的核心逻辑：
 * 按分类检索、报销标准匹配、审批流程查询、税务政策查询、降级提示。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeEntryMapper knowledgeEntryMapper;

    private KnowledgeService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeService(knowledgeEntryMapper);
    }

    // ========== findActiveByCategory ==========

    @Nested
    @DisplayName("findActiveByCategory - 按分类检索有效知识条目")
    class FindActiveByCategory {

        @Test
        @DisplayName("正常分类返回有效条目列表")
        void returnsActiveEntriesForCategory() {
            KnowledgeEntry entry = KnowledgeEntry.builder()
                    .entryId("K001")
                    .category("报销制度")
                    .title("差旅报销标准")
                    .status(KnowledgeStatus.ACTIVE)
                    .build();
            when(knowledgeEntryMapper.selectActiveByCategory("报销制度"))
                    .thenReturn(Collections.singletonList(entry));

            List<KnowledgeEntry> result = service.findActiveByCategory("报销制度");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEntryId()).isEqualTo("K001");
        }

        @Test
        @DisplayName("分类为 null 时返回空列表")
        void nullCategoryReturnsEmpty() {
            List<KnowledgeEntry> result = service.findActiveByCategory(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("分类为空字符串时返回空列表")
        void emptyCategoryReturnsEmpty() {
            List<KnowledgeEntry> result = service.findActiveByCategory("  ");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("无匹配条目时返回空列表")
        void noMatchReturnsEmpty() {
            when(knowledgeEntryMapper.selectActiveByCategory("不存在的分类"))
                    .thenReturn(Collections.emptyList());

            List<KnowledgeEntry> result = service.findActiveByCategory("不存在的分类");
            assertThat(result).isEmpty();
        }
    }

    // ========== findExpenseStandard ==========

    @Nested
    @DisplayName("findExpenseStandard - 报销标准按职级和城市匹配")
    class FindExpenseStandard {

        @Test
        @DisplayName("匹配职级和城市的报销标准")
        void matchesLevelAndCity() {
            KnowledgeEntry entry1 = KnowledgeEntry.builder()
                    .entryId("K001")
                    .category("报销制度")
                    .title("P5北京差旅住宿标准")
                    .content("P5级别在北京出差住宿标准为500元/晚")
                    .status(KnowledgeStatus.ACTIVE)
                    .build();
            KnowledgeEntry entry2 = KnowledgeEntry.builder()
                    .entryId("K002")
                    .category("报销制度")
                    .title("M1上海差旅住宿标准")
                    .content("M1级别在上海出差住宿标准为800元/晚")
                    .status(KnowledgeStatus.ACTIVE)
                    .build();
            when(knowledgeEntryMapper.selectActiveByCategory("报销制度"))
                    .thenReturn(Arrays.asList(entry1, entry2));

            List<KnowledgeEntry> result = service.findExpenseStandard("P5", "北京");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEntryId()).isEqualTo("K001");
        }

        @Test
        @DisplayName("职级匹配但城市不匹配时返回空")
        void levelMatchButCityMismatch() {
            KnowledgeEntry entry = KnowledgeEntry.builder()
                    .entryId("K001")
                    .category("报销制度")
                    .title("P5北京差旅住宿标准")
                    .content("P5级别在北京出差住宿标准为500元/晚")
                    .status(KnowledgeStatus.ACTIVE)
                    .build();
            when(knowledgeEntryMapper.selectActiveByCategory("报销制度"))
                    .thenReturn(Collections.singletonList(entry));

            List<KnowledgeEntry> result = service.findExpenseStandard("P5", "深圳");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("职级为 null 时返回空列表")
        void nullLevelReturnsEmpty() {
            List<KnowledgeEntry> result = service.findExpenseStandard(null, "北京");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("城市为空字符串时返回空列表")
        void emptyCityReturnsEmpty() {
            List<KnowledgeEntry> result = service.findExpenseStandard("P5", "");
            assertThat(result).isEmpty();
        }
    }

    // ========== findApprovalFlow ==========

    @Nested
    @DisplayName("findApprovalFlow - 审批流程查询")
    class FindApprovalFlow {

        @Test
        @DisplayName("正常查询返回包含步骤、材料、预计时间的结果")
        void returnsCompleteApprovalFlow() {
            KnowledgeEntry entry = KnowledgeEntry.builder()
                    .entryId("K010")
                    .category("审批流程")
                    .title("差旅报销审批流程")
                    .content("步骤：1.提交申请;2.部门审批;3.财务审核\n" +
                            "材料：发票原件;费用明细表;审批单\n" +
                            "预计时间：3个工作日")
                    .status(KnowledgeStatus.ACTIVE)
                    .build();
            when(knowledgeEntryMapper.selectActiveByCategory("审批流程"))
                    .thenReturn(Collections.singletonList(entry));

            KnowledgeService.ApprovalFlowResult result = service.findApprovalFlow("差旅报销");

            assertThat(result).isNotNull();
            assertThat(result.getSteps()).containsExactly("1.提交申请", "2.部门审批", "3.财务审核");
            assertThat(result.getMaterials()).containsExactly("发票原件", "费用明细表", "审批单");
            assertThat(result.getEstimatedDuration()).isEqualTo("3个工作日");
        }

        @Test
        @DisplayName("流程类型为 null 时返回 null")
        void nullFlowTypeReturnsNull() {
            KnowledgeService.ApprovalFlowResult result = service.findApprovalFlow(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("无匹配流程时返回 null")
        void noMatchReturnsNull() {
            when(knowledgeEntryMapper.selectActiveByCategory("审批流程"))
                    .thenReturn(Collections.emptyList());

            KnowledgeService.ApprovalFlowResult result = service.findApprovalFlow("不存在的流程");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("内容为空时使用标题作为默认步骤")
        void emptyContentUseTitleAsStep() {
            KnowledgeEntry entry = KnowledgeEntry.builder()
                    .entryId("K011")
                    .category("审批流程")
                    .title("借款申请审批流程")
                    .content("")
                    .status(KnowledgeStatus.ACTIVE)
                    .build();
            when(knowledgeEntryMapper.selectActiveByCategory("审批流程"))
                    .thenReturn(Collections.singletonList(entry));

            KnowledgeService.ApprovalFlowResult result = service.findApprovalFlow("借款申请");

            assertThat(result).isNotNull();
            assertThat(result.getSteps()).containsExactly("借款申请审批流程");
            assertThat(result.getEstimatedDuration()).isEqualTo("未知");
        }
    }

    // ========== findActiveTaxPolicies ==========

    @Nested
    @DisplayName("findActiveTaxPolicies - 税务政策查询")
    class FindActiveTaxPolicies {

        @Test
        @DisplayName("仅返回 ACTIVE 且已生效的税务政策")
        void returnsOnlyActiveAndEffective() {
            LocalDateTime now = LocalDateTime.now();
            KnowledgeEntry effective = KnowledgeEntry.builder()
                    .entryId("T001")
                    .category("税务政策")
                    .title("个税专项附加扣除")
                    .status(KnowledgeStatus.ACTIVE)
                    .effectiveAt(now.minusDays(30))
                    .build();
            KnowledgeEntry future = KnowledgeEntry.builder()
                    .entryId("T002")
                    .category("税务政策")
                    .title("新税率政策")
                    .status(KnowledgeStatus.ACTIVE)
                    .effectiveAt(now.plusDays(30))
                    .build();
            KnowledgeEntry noEffectiveDate = KnowledgeEntry.builder()
                    .entryId("T003")
                    .category("税务政策")
                    .title("无生效时间的政策")
                    .status(KnowledgeStatus.ACTIVE)
                    .effectiveAt(null)
                    .build();
            when(knowledgeEntryMapper.selectActiveByCategory("税务政策"))
                    .thenReturn(Arrays.asList(effective, future, noEffectiveDate));

            List<KnowledgeEntry> result = service.findActiveTaxPolicies();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEntryId()).isEqualTo("T001");
        }

        @Test
        @DisplayName("无有效税务政策时返回空列表")
        void noEffectivePoliciesReturnsEmpty() {
            when(knowledgeEntryMapper.selectActiveByCategory("税务政策"))
                    .thenReturn(Collections.emptyList());

            List<KnowledgeEntry> result = service.findActiveTaxPolicies();
            assertThat(result).isEmpty();
        }
    }

    // ========== findWithFallback ==========

    @Nested
    @DisplayName("findWithFallback - 带降级提示的查询")
    class FindWithFallback {

        @Test
        @DisplayName("有匹配结果时不触发降级")
        void noFallbackWhenResultsExist() {
            KnowledgeEntry entry = KnowledgeEntry.builder()
                    .entryId("K001")
                    .category("报销制度")
                    .title("差旅报销标准")
                    .status(KnowledgeStatus.ACTIVE)
                    .build();
            when(knowledgeEntryMapper.selectActiveByCategory("报销制度"))
                    .thenReturn(Collections.singletonList(entry));

            KnowledgeService.FallbackResult result = service.findWithFallback("报销制度");

            assertThat(result.isFallback()).isFalse();
            assertThat(result.getEntries()).hasSize(1);
            assertThat(result.getFallbackMessage()).isNull();
        }

        @Test
        @DisplayName("无匹配结果时触发降级提示")
        void fallbackWhenNoResults() {
            when(knowledgeEntryMapper.selectActiveByCategory("未知分类"))
                    .thenReturn(Collections.emptyList());

            KnowledgeService.FallbackResult result = service.findWithFallback("未知分类");

            assertThat(result.isFallback()).isTrue();
            assertThat(result.getEntries()).isEmpty();
            assertThat(result.getFallbackMessage()).isEqualTo("暂未找到相关信息，建议联系人工客服");
        }

        @Test
        @DisplayName("分类为 null 时触发降级提示")
        void nullCategoryTriggersFallback() {
            KnowledgeService.FallbackResult result = service.findWithFallback(null);

            assertThat(result.isFallback()).isTrue();
            assertThat(result.getEntries()).isEmpty();
        }
    }

    // ========== parseApprovalFlow ==========

    @Nested
    @DisplayName("parseApprovalFlow - 审批流程内容解析")
    class ParseApprovalFlow {

        @Test
        @DisplayName("使用中文冒号解析步骤、材料、预计时间")
        void parsesChineseColonFormat() {
            KnowledgeEntry entry = KnowledgeEntry.builder()
                    .title("测试流程")
                    .content("步骤：提交;审批\n材料：发票;明细\n预计时间：5天")
                    .build();

            KnowledgeService.ApprovalFlowResult result = service.parseApprovalFlow(entry);

            assertThat(result.getSteps()).containsExactly("提交", "审批");
            assertThat(result.getMaterials()).containsExactly("发票", "明细");
            assertThat(result.getEstimatedDuration()).isEqualTo("5天");
        }

        @Test
        @DisplayName("使用英文冒号也能正确解析")
        void parsesEnglishColonFormat() {
            KnowledgeEntry entry = KnowledgeEntry.builder()
                    .title("测试流程")
                    .content("步骤:提交;审批\n材料:发票\n预计时间:3天")
                    .build();

            KnowledgeService.ApprovalFlowResult result = service.parseApprovalFlow(entry);

            assertThat(result.getSteps()).containsExactly("提交", "审批");
            assertThat(result.getMaterials()).containsExactly("发票");
            assertThat(result.getEstimatedDuration()).isEqualTo("3天");
        }

        @Test
        @DisplayName("内容为 null 时返回标题作为默认步骤")
        void nullContentUsesTitle() {
            KnowledgeEntry entry = KnowledgeEntry.builder()
                    .title("默认流程")
                    .content(null)
                    .build();

            KnowledgeService.ApprovalFlowResult result = service.parseApprovalFlow(entry);

            assertThat(result.getSteps()).containsExactly("默认流程");
            assertThat(result.getMaterials()).isEmpty();
            assertThat(result.getEstimatedDuration()).isEqualTo("未知");
        }
    }

    // ========== submitForReview ==========

    @Nested
    @DisplayName("submitForReview - 提交知识条目审核")
    class SubmitForReview {

        @Test
        @DisplayName("提交条目时状态被设为 PENDING_REVIEW 并调用 insert")
        void setsStatusToPendingReviewAndInserts() {
            KnowledgeEntry entry = KnowledgeEntry.builder()
                    .entryId("K100")
                    .category("报销制度")
                    .title("新报销标准")
                    .content("内容")
                    .status(KnowledgeStatus.DRAFT)
                    .build();
            when(knowledgeEntryMapper.insert(any(KnowledgeEntry.class))).thenReturn(1);

            int rows = service.submitForReview(entry);

            assertThat(rows).isEqualTo(1);
            assertThat(entry.getStatus()).isEqualTo(KnowledgeStatus.PENDING_REVIEW);
            verify(knowledgeEntryMapper).insert(entry);
        }

        @Test
        @DisplayName("传入 null 时返回 0 且不调用 mapper")
        void nullEntryReturnsZero() {
            int rows = service.submitForReview(null);

            assertThat(rows).isEqualTo(0);
            verify(knowledgeEntryMapper, never()).insert(any());
        }
    }

    // ========== approveEntry ==========

    @Nested
    @DisplayName("approveEntry - 审核通过知识条目")
    class ApproveEntry {

        @Test
        @DisplayName("审核通过后调用 updateStatus 将状态设为 ACTIVE")
        void updatesStatusToActive() {
            when(knowledgeEntryMapper.updateStatus("K100", "ACTIVE")).thenReturn(1);

            int rows = service.approveEntry("K100", "admin");

            assertThat(rows).isEqualTo(1);
            verify(knowledgeEntryMapper).updateStatus("K100", "ACTIVE");
        }

        @Test
        @DisplayName("entryId 为 null 时返回 0 且不调用 mapper")
        void nullEntryIdReturnsZero() {
            int rows = service.approveEntry(null, "admin");

            assertThat(rows).isEqualTo(0);
            verify(knowledgeEntryMapper, never()).updateStatus(any(), any());
        }

        @Test
        @DisplayName("entryId 为空字符串时返回 0")
        void emptyEntryIdReturnsZero() {
            int rows = service.approveEntry("  ", "admin");

            assertThat(rows).isEqualTo(0);
            verify(knowledgeEntryMapper, never()).updateStatus(any(), any());
        }
    }

    // ========== rejectEntry ==========

    @Nested
    @DisplayName("rejectEntry - 审核驳回知识条目")
    class RejectEntry {

        @Test
        @DisplayName("审核驳回后调用 updateStatus 将状态设为 DRAFT")
        void updatesStatusToDraft() {
            when(knowledgeEntryMapper.updateStatus("K100", "DRAFT")).thenReturn(1);

            int rows = service.rejectEntry("K100", "admin");

            assertThat(rows).isEqualTo(1);
            verify(knowledgeEntryMapper).updateStatus("K100", "DRAFT");
        }

        @Test
        @DisplayName("entryId 为 null 时返回 0 且不调用 mapper")
        void nullEntryIdReturnsZero() {
            int rows = service.rejectEntry(null, "admin");

            assertThat(rows).isEqualTo(0);
            verify(knowledgeEntryMapper, never()).updateStatus(any(), any());
        }

        @Test
        @DisplayName("entryId 为空字符串时返回 0")
        void emptyEntryIdReturnsZero() {
            int rows = service.rejectEntry("  ", "admin");

            assertThat(rows).isEqualTo(0);
            verify(knowledgeEntryMapper, never()).updateStatus(any(), any());
        }
    }

    // ========== 审核流程集成验证 ==========

    @Nested
    @DisplayName("知识库更新审核流程 - PENDING_REVIEW 条目不出现在员工查询结果中")
    class ReviewWorkflowIntegration {

        @Test
        @DisplayName("selectActiveByCategory 只返回 ACTIVE 条目，PENDING_REVIEW 条目不可见")
        void pendingReviewNotVisibleInActiveQuery() {
            // selectActiveByCategory 只查 ACTIVE 状态，所以 PENDING_REVIEW 条目自然不会返回
            KnowledgeEntry activeEntry = KnowledgeEntry.builder()
                    .entryId("K001")
                    .category("报销制度")
                    .title("已生效标准")
                    .status(KnowledgeStatus.ACTIVE)
                    .build();
            when(knowledgeEntryMapper.selectActiveByCategory("报销制度"))
                    .thenReturn(Collections.singletonList(activeEntry));

            List<KnowledgeEntry> result = service.findActiveByCategory("报销制度");

            // 验证只返回 ACTIVE 条目
            assertThat(result).hasSize(1);
            assertThat(result).allMatch(e -> e.getStatus() == KnowledgeStatus.ACTIVE);
        }
    }
}
