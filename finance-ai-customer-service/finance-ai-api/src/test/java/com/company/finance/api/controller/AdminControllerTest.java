package com.company.finance.api.controller;

import com.company.finance.api.controller.AdminController.ReviewRequest;
import com.company.finance.api.security.UserPrincipal;
import com.company.finance.common.dto.AuditLogQuery;
import com.company.finance.domain.entity.AuditLog;
import com.company.finance.domain.entity.KnowledgeEntry;
import com.company.finance.domain.entity.OperationMetrics;
import com.company.finance.service.audit.AuditLogSearchService;
import com.company.finance.service.autoreply.AutoReplyRule;
import com.company.finance.service.autoreply.AutoReplyRuleService;
import com.company.finance.service.knowledge.KnowledgeCategoryService;
import com.company.finance.service.knowledge.KnowledgeService;
import com.company.finance.service.operation.OperationService;
import com.company.finance.service.operation.OperationService.HotTopic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AdminController 单元测试
 */
class AdminControllerTest {

    private AuditLogSearchService auditLogSearchService;
    private OperationService operationService;
    private KnowledgeCategoryService knowledgeCategoryService;
    private KnowledgeService knowledgeService;
    private AutoReplyRuleService autoReplyRuleService;
    private AdminController adminController;
    private UserPrincipal operatorUser;

    @BeforeEach
    void setUp() {
        auditLogSearchService = mock(AuditLogSearchService.class);
        operationService = mock(OperationService.class);
        knowledgeCategoryService = mock(KnowledgeCategoryService.class);
        knowledgeService = mock(KnowledgeService.class);
        autoReplyRuleService = mock(AutoReplyRuleService.class);
        adminController = new AdminController(
                auditLogSearchService, operationService, knowledgeCategoryService, knowledgeService, autoReplyRuleService);
        operatorUser = new UserPrincipal("OP001", "DEPT-OPS", Collections.singletonList("ROLE_OPERATOR"));
    }

    // ==================== queryLogs ====================

    @Test
    void queryLogsShouldReturnFilteredLogs() {
        AuditLog log1 = AuditLog.builder()
                .logId("log-001").employeeId("EMP001").action("CHAT")
                .requestContent("报销查询").timestamp(LocalDateTime.now()).build();

        when(auditLogSearchService.search(any(AuditLogQuery.class)))
                .thenReturn(Collections.singletonList(log1));

        Mono<ResponseEntity<List<AuditLog>>> result =
                adminController.queryLogs(null, null, "EMP001", null, null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).hasSize(1);
                    assertThat(response.getBody().get(0).getEmployeeId()).isEqualTo("EMP001");
                })
                .verifyComplete();
    }

    @Test
    void queryLogsShouldReturnEmptyListWhenNoMatch() {
        when(auditLogSearchService.search(any(AuditLogQuery.class)))
                .thenReturn(Collections.emptyList());

        Mono<ResponseEntity<List<AuditLog>>> result =
                adminController.queryLogs(null, null, null, null, null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void queryLogsShouldPassAllParametersToService() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59, 59);

        when(auditLogSearchService.search(any(AuditLogQuery.class)))
                .thenReturn(Collections.emptyList());

        adminController.queryLogs(start, end, "EMP001", "EXPENSE_QUERY", "sess-001").block();

        ArgumentCaptor<AuditLogQuery> captor = ArgumentCaptor.forClass(AuditLogQuery.class);
        verify(auditLogSearchService).search(captor.capture());
        AuditLogQuery captured = captor.getValue();
        assertThat(captured.getStartTime()).isEqualTo(start);
        assertThat(captured.getEndTime()).isEqualTo(end);
        assertThat(captured.getEmployeeId()).isEqualTo("EMP001");
        assertThat(captured.getIntent()).isEqualTo("EXPENSE_QUERY");
        assertThat(captured.getSessionId()).isEqualTo("sess-001");
    }

    // ==================== getHotTopics ====================

    @Test
    void getHotTopicsShouldReturnTopicsForDate() {
        LocalDate startDate = LocalDate.of(2024, 6, 1);
        LocalDate endDate = LocalDate.of(2024, 6, 1);
        List<HotTopic> topics = Arrays.asList(
                new HotTopic("报销查询", 50),
                new HotTopic("发票验真", 30));

        when(operationService.calculateHotTopics(startDate, endDate)).thenReturn(topics);

        Mono<ResponseEntity<List<HotTopic>>> result = adminController.getHotTopics(startDate, endDate);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).hasSize(2);
                    assertThat(response.getBody().get(0).getTopic()).isEqualTo("报销查询");
                    assertThat(response.getBody().get(0).getCount()).isEqualTo(50);
                })
                .verifyComplete();
    }

    @Test
    void getHotTopicsShouldDefaultToTodayWhenNoDate() {
        when(operationService.calculateHotTopics(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        adminController.getHotTopics(null, null).block();

        verify(operationService).calculateHotTopics(LocalDate.now(), LocalDate.now());
    }

    // ==================== getMetrics ====================

    @Test
    void getMetricsShouldReturnMetricsForDate() {
        LocalDate date = LocalDate.of(2024, 6, 1);
        OperationMetrics metrics = OperationMetrics.builder()
                .metricId("m-001").date(date)
                .totalSessions(100).selfResolvedSessions(80)
                .humanHandoffSessions(20).avgResponseTimeMs(1500)
                .satisfactionScore(4.5).build();

        when(operationService.calculateMetrics(date)).thenReturn(metrics);

        Mono<ResponseEntity<OperationMetrics>> result = adminController.getMetrics(date);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody().getTotalSessions()).isEqualTo(100);
                    assertThat(response.getBody().getSelfResolvedSessions()).isEqualTo(80);
                    assertThat(response.getBody().getSatisfactionScore()).isEqualTo(4.5);
                })
                .verifyComplete();
    }

    @Test
    void getMetricsShouldDefaultToTodayWhenNoDate() {
        OperationMetrics metrics = OperationMetrics.builder()
                .metricId("m-002").date(LocalDate.now())
                .totalSessions(0).build();

        when(operationService.calculateMetrics(any(LocalDate.class))).thenReturn(metrics);

        adminController.getMetrics(null).block();

        verify(operationService).calculateMetrics(LocalDate.now());
    }

    // ==================== addKnowledgeEntry ====================

    @Test
    void addKnowledgeEntryShouldSubmitForReview() {
        KnowledgeEntry entry = KnowledgeEntry.builder()
                .entryId("ke-001").category("报销制度")
                .title("差旅住宿标准").content("一线城市500元/晚")
                .build();

        when(knowledgeService.submitForReview(any(KnowledgeEntry.class))).thenReturn(1);

        Mono<ResponseEntity<KnowledgeEntry>> result =
                adminController.addKnowledgeEntry(entry, operatorUser);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody().getCreatedBy()).isEqualTo("OP001");
                    assertThat(response.getBody().getCategory()).isEqualTo("报销制度");
                })
                .verifyComplete();

        verify(knowledgeService).submitForReview(any(KnowledgeEntry.class));
    }

    // ==================== reviewKnowledgeEntry ====================

    @Test
    void reviewKnowledgeEntryShouldApproveWhenApprovedTrue() {
        when(knowledgeService.approveEntry("ke-001", "OP001")).thenReturn(1);

        ReviewRequest request = new ReviewRequest(true);
        Mono<ResponseEntity<Integer>> result =
                adminController.reviewKnowledgeEntry("ke-001", request, operatorUser);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).isEqualTo(1);
                })
                .verifyComplete();

        verify(knowledgeService).approveEntry("ke-001", "OP001");
    }

    @Test
    void reviewKnowledgeEntryShouldRejectWhenApprovedFalse() {
        when(knowledgeService.rejectEntry("ke-001", "OP001")).thenReturn(1);

        ReviewRequest request = new ReviewRequest(false);
        Mono<ResponseEntity<Integer>> result =
                adminController.reviewKnowledgeEntry("ke-001", request, operatorUser);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).isEqualTo(1);
                })
                .verifyComplete();

        verify(knowledgeService).rejectEntry("ke-001", "OP001");
    }

    @Test
    void reviewKnowledgeEntryShouldReturn404WhenEntryNotFound() {
        when(knowledgeService.approveEntry("nonexistent", "OP001")).thenReturn(0);

        ReviewRequest request = new ReviewRequest(true);
        Mono<ResponseEntity<Integer>> result =
                adminController.reviewKnowledgeEntry("nonexistent", request, operatorUser);

        StepVerifier.create(result)
                .assertNext(response -> assertThat(response.getStatusCodeValue()).isEqualTo(404))
                .verifyComplete();
    }

    // ==================== getAutoReplyRules ====================

    @Test
    void getAutoReplyRulesShouldReturnAllRules() {
        AutoReplyRule rule1 = AutoReplyRule.builder()
                .ruleId("rule-001").name("报销咨询")
                .keywords(Arrays.asList("报销", "费用"))
                .replyTemplate("请问您需要查询哪笔报销？")
                .enabled(true).priority(1).build();

        when(autoReplyRuleService.getAllRules()).thenReturn(Collections.singletonList(rule1));

        Mono<ResponseEntity<List<AutoReplyRule>>> result = adminController.getAutoReplyRules();

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).hasSize(1);
                    assertThat(response.getBody().get(0).getName()).isEqualTo("报销咨询");
                })
                .verifyComplete();
    }

    @Test
    void getAutoReplyRulesShouldReturnEmptyListWhenNoRules() {
        when(autoReplyRuleService.getAllRules()).thenReturn(Collections.emptyList());

        Mono<ResponseEntity<List<AutoReplyRule>>> result = adminController.getAutoReplyRules();

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody()).isEmpty();
                })
                .verifyComplete();
    }

    // ==================== addAutoReplyRule ====================

    @Test
    void addAutoReplyRuleShouldCreateRule() {
        AutoReplyRule rule = AutoReplyRule.builder()
                .name("发票咨询")
                .keywords(Arrays.asList("发票", "开票"))
                .replyTemplate("请提供发票号码")
                .enabled(true).priority(2).build();

        AutoReplyRule created = AutoReplyRule.builder()
                .ruleId("rule-new")
                .name("发票咨询")
                .keywords(Arrays.asList("发票", "开票"))
                .replyTemplate("请提供发票号码")
                .enabled(true).priority(2).build();

        when(autoReplyRuleService.addRule(any(AutoReplyRule.class))).thenReturn(created);

        Mono<ResponseEntity<AutoReplyRule>> result = adminController.addAutoReplyRule(rule);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCodeValue()).isEqualTo(200);
                    assertThat(response.getBody().getRuleId()).isEqualTo("rule-new");
                    assertThat(response.getBody().getName()).isEqualTo("发票咨询");
                })
                .verifyComplete();
    }
}
