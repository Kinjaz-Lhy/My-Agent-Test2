package com.company.finance.api.controller;

import com.company.finance.api.security.UserPrincipal;
import com.company.finance.common.dto.AuditLogQuery;
import com.company.finance.domain.entity.AuditLog;
import com.company.finance.domain.entity.KnowledgeEntry;
import com.company.finance.domain.entity.OperationMetrics;
import com.company.finance.service.audit.AuditLogSearchService;
import com.company.finance.service.autoreply.AutoReplyRule;
import com.company.finance.service.autoreply.AutoReplyRuleService;
import com.company.finance.service.knowledge.KnowledgeService;
import com.company.finance.service.operation.OperationService;
import com.company.finance.service.operation.OperationService.HotTopic;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 运营管理控制器
 * <p>
 * 提供对话日志查询、热点问题统计、运营指标看板、知识库管理和自动回复规则配置端点。
 * 所有端点需要 OPERATOR 角色（Spring Security 配置 /api/v1/admin/** → OPERATOR）。
 * </p>
 *
 * @see <a href="需求 6.1, 6.2, 6.4, 6.5, 6.6">运营管理</a>
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AuditLogSearchService auditLogSearchService;
    private final OperationService operationService;
    private final KnowledgeService knowledgeService;
    private final AutoReplyRuleService autoReplyRuleService;

    public AdminController(AuditLogSearchService auditLogSearchService,
                           OperationService operationService,
                           KnowledgeService knowledgeService,
                           AutoReplyRuleService autoReplyRuleService) {
        this.auditLogSearchService = auditLogSearchService;
        this.operationService = operationService;
        this.knowledgeService = knowledgeService;
        this.autoReplyRuleService = autoReplyRuleService;
    }

    private UserPrincipal resolveUser(UserPrincipal user) {
        if (user != null) return user;
        return new UserPrincipal("dev-operator", "DEPT-DEV", Collections.singletonList("ROLE_OPERATOR"));
    }

    // ==================== 对话日志查询 ====================

    /**
     * 对话日志查询
     * <p>
     * 支持按时间范围、用户、意图分类、会话 ID 多条件组合检索。
     * 所有参数均为可选。
     * </p>
     *
     * @param startTime  起始时间（可选）
     * @param endTime    结束时间（可选）
     * @param employeeId 员工 ID（可选）
     * @param intent     意图分类（可选）
     * @param sessionId  会话 ID（可选）
     * @return 审计日志列表
     */
    @GetMapping("/logs")
    public Mono<ResponseEntity<List<AuditLog>>> queryLogs(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String intent,
            @RequestParam(required = false) String sessionId) {
        AuditLogQuery query = AuditLogQuery.builder()
                .startTime(startTime)
                .endTime(endTime)
                .employeeId(employeeId)
                .intent(intent)
                .sessionId(sessionId)
                .build();
        List<AuditLog> logs = auditLogSearchService.search(query);
        return Mono.just(ResponseEntity.ok(logs));
    }

    // ==================== 热点问题统计 ====================

    /**
     * 热点问题统计
     * <p>
     * 返回指定日期的前 20 个高频咨询问题，按频次降序排列。
     * 日期参数可选，默认为当天。
     * </p>
     *
     * @param date 统计日期（可选，默认当天）
     * @return 热点问题列表
     */
    @GetMapping("/hot-topics")
    public Mono<ResponseEntity<List<HotTopic>>> getHotTopics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<HotTopic> hotTopics = operationService.calculateHotTopics(targetDate);
        return Mono.just(ResponseEntity.ok(hotTopics));
    }

    // ==================== 运营指标看板 ====================

    /**
     * 运营指标看板数据
     * <p>
     * 返回指定日期的运营指标，包含服务量、自助解决率、人工转接率、平均响应时间等。
     * 日期参数可选，默认为当天。
     * </p>
     *
     * @param date 统计日期（可选，默认当天）
     * @return 运营指标
     */
    @GetMapping("/metrics")
    public Mono<ResponseEntity<OperationMetrics>> getMetrics(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        OperationMetrics metrics = operationService.calculateMetrics(targetDate);
        return Mono.just(ResponseEntity.ok(metrics));
    }

    // ==================== 知识库管理 ====================

    /**
     * 新增知识条目
     * <p>
     * 提交新的知识条目，状态自动置为 PENDING_REVIEW，待审核通过后生效。
     * </p>
     *
     * @param entry 知识条目
     * @param user  当前认证用户（运营人员）
     * @return 提交结果（影响行数）
     */
    @PostMapping("/knowledge")
    public Mono<ResponseEntity<KnowledgeEntry>> addKnowledgeEntry(
            @Valid @RequestBody KnowledgeEntry entry,
            @AuthenticationPrincipal UserPrincipal user) {
        entry.setCreatedBy(resolveUser(user).getEmployeeId());
        knowledgeService.submitForReview(entry);
        return Mono.just(ResponseEntity.ok(entry));
    }

    /**
     * 审核知识条目
     * <p>
     * 审核通过后知识条目状态变为 ACTIVE，可被员工查询到。
     * 请求体需包含 approved 字段：true 为通过，false 为驳回。
     * </p>
     *
     * @param entryId 知识条目 ID
     * @param request 审核请求（含 approved 字段）
     * @param user    当前认证用户（审核人）
     * @return 更新影响行数
     */
    @PutMapping("/knowledge/{entryId}/review")
    public Mono<ResponseEntity<Integer>> reviewKnowledgeEntry(
            @PathVariable String entryId,
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        String reviewerId = resolveUser(user).getEmployeeId();
        int rows;
        if (request.isApproved()) {
            rows = knowledgeService.approveEntry(entryId, reviewerId);
        } else {
            rows = knowledgeService.rejectEntry(entryId, reviewerId);
        }
        if (rows == 0) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        return Mono.just(ResponseEntity.ok(rows));
    }

    // ==================== 自动回复规则 ====================

    /**
     * 获取所有自动回复规则（按优先级排序）
     *
     * @return 规则列表
     */
    @GetMapping("/auto-reply-rules")
    public Mono<ResponseEntity<List<AutoReplyRule>>> getAutoReplyRules() {
        List<AutoReplyRule> rules = autoReplyRuleService.getAllRules();
        return Mono.just(ResponseEntity.ok(rules));
    }

    /**
     * 配置自动回复规则（新增）
     *
     * @param rule 规则对象
     * @return 创建后的规则（含生成的 ruleId）
     */
    @PostMapping("/auto-reply-rules")
    public Mono<ResponseEntity<AutoReplyRule>> addAutoReplyRule(
            @Valid @RequestBody AutoReplyRule rule) {
        AutoReplyRule created = autoReplyRuleService.addRule(rule);
        return Mono.just(ResponseEntity.ok(created));
    }

    // ==================== 内部 DTO ====================

    /**
     * 审核请求 DTO
     */
    public static class ReviewRequest {
        private boolean approved;

        public ReviewRequest() {
        }

        public ReviewRequest(boolean approved) {
            this.approved = approved;
        }

        public boolean isApproved() {
            return approved;
        }

        public void setApproved(boolean approved) {
            this.approved = approved;
        }
    }
}
