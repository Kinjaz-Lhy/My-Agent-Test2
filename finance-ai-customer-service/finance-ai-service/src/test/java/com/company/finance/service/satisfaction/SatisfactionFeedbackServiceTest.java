package com.company.finance.service.satisfaction;

import com.company.finance.domain.entity.SatisfactionFeedback;
import com.company.finance.infrastructure.mapper.SatisfactionFeedbackMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SatisfactionFeedbackService 单元测试
 * <p>
 * 验证满意度评价提交、评分校验、反馈存在性检查等核心逻辑。
 * </p>
 *
 * @see <a href="需求 6.3">满意度评价</a>
 */
@DisplayName("SatisfactionFeedbackService - 满意度评价服务")
class SatisfactionFeedbackServiceTest {

    private SatisfactionFeedbackMapper satisfactionFeedbackMapper;
    private SatisfactionFeedbackService satisfactionFeedbackService;

    @BeforeEach
    void setUp() {
        satisfactionFeedbackMapper = mock(SatisfactionFeedbackMapper.class);
        satisfactionFeedbackService = new SatisfactionFeedbackService(satisfactionFeedbackMapper);
    }

    // ========== submitFeedback 测试 ==========

    @Test
    @DisplayName("提交满意度评价 - 有效评分应成功持久化")
    void submitFeedback_withValidScore_shouldPersist() {
        when(satisfactionFeedbackMapper.insert(any(SatisfactionFeedback.class))).thenReturn(1);

        SatisfactionFeedback result = satisfactionFeedbackService.submitFeedback(
                "session-001", "EMP001", 5, "非常满意");

        assertThat(result).isNotNull();
        assertThat(result.getFeedbackId()).isNotNull().isNotEmpty();
        assertThat(result.getSessionId()).isEqualTo("session-001");
        assertThat(result.getEmployeeId()).isEqualTo("EMP001");
        assertThat(result.getScore()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("非常满意");
        assertThat(result.getCreatedAt()).isNotNull();

        verify(satisfactionFeedbackMapper).insert(any(SatisfactionFeedback.class));
    }

    @Test
    @DisplayName("提交满意度评价 - 最低分 1 分应成功")
    void submitFeedback_withMinScore_shouldSucceed() {
        when(satisfactionFeedbackMapper.insert(any(SatisfactionFeedback.class))).thenReturn(1);

        SatisfactionFeedback result = satisfactionFeedbackService.submitFeedback(
                "session-002", "EMP002", 1, null);

        assertThat(result.getScore()).isEqualTo(1);
        assertThat(result.getComment()).isNull();
        verify(satisfactionFeedbackMapper).insert(any(SatisfactionFeedback.class));
    }

    @Test
    @DisplayName("提交满意度评价 - 评分低于 1 应抛出异常")
    void submitFeedback_withScoreBelowMin_shouldThrow() {
        assertThatThrownBy(() ->
                satisfactionFeedbackService.submitFeedback("session-003", "EMP003", 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("评分必须在 1 到 5 之间");

        verify(satisfactionFeedbackMapper, never()).insert(any());
    }

    @Test
    @DisplayName("提交满意度评价 - 评分高于 5 应抛出异常")
    void submitFeedback_withScoreAboveMax_shouldThrow() {
        assertThatThrownBy(() ->
                satisfactionFeedbackService.submitFeedback("session-004", "EMP004", 6, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("评分必须在 1 到 5 之间");

        verify(satisfactionFeedbackMapper, never()).insert(any());
    }

    @Test
    @DisplayName("提交满意度评价 - 无文字反馈（null comment）应成功")
    void submitFeedback_withNullComment_shouldSucceed() {
        when(satisfactionFeedbackMapper.insert(any(SatisfactionFeedback.class))).thenReturn(1);

        SatisfactionFeedback result = satisfactionFeedbackService.submitFeedback(
                "session-005", "EMP005", 3, null);

        assertThat(result.getComment()).isNull();
        verify(satisfactionFeedbackMapper).insert(any(SatisfactionFeedback.class));
    }

    // ========== feedbackExists 测试 ==========

    @Test
    @DisplayName("检查反馈存在性 - 已存在反馈时返回 true")
    void feedbackExists_whenFeedbackPresent_shouldReturnTrue() {
        SatisfactionFeedback existing = SatisfactionFeedback.builder()
                .feedbackId("fb-001")
                .sessionId("session-010")
                .employeeId("EMP010")
                .score(4)
                .createdAt(LocalDateTime.now())
                .build();
        when(satisfactionFeedbackMapper.selectBySessionId("session-010")).thenReturn(existing);

        assertThat(satisfactionFeedbackService.feedbackExists("session-010")).isTrue();
    }

    @Test
    @DisplayName("检查反馈存在性 - 不存在反馈时返回 false")
    void feedbackExists_whenNoFeedback_shouldReturnFalse() {
        when(satisfactionFeedbackMapper.selectBySessionId("session-011")).thenReturn(null);

        assertThat(satisfactionFeedbackService.feedbackExists("session-011")).isFalse();
    }

    // ========== getAverageScore 测试 ==========

    @Test
    @DisplayName("查询平均评分 - 有数据时返回平均值")
    void getAverageScore_withData_shouldReturnAverage() {
        when(satisfactionFeedbackMapper.selectAvgScoreByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(4.2);

        Double avg = satisfactionFeedbackService.getAverageScore(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        assertThat(avg).isEqualTo(4.2);
    }

    @Test
    @DisplayName("查询平均评分 - 无数据时返回 null")
    void getAverageScore_withNoData_shouldReturnNull() {
        when(satisfactionFeedbackMapper.selectAvgScoreByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(null);

        Double avg = satisfactionFeedbackService.getAverageScore(
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30));

        assertThat(avg).isNull();
    }

    // ========== isValidScore 测试 ==========

    @Test
    @DisplayName("评分有效性 - 边界值验证")
    void isValidScore_shouldValidateBoundaries() {
        assertThat(SatisfactionFeedbackService.isValidScore(0)).isFalse();
        assertThat(SatisfactionFeedbackService.isValidScore(1)).isTrue();
        assertThat(SatisfactionFeedbackService.isValidScore(3)).isTrue();
        assertThat(SatisfactionFeedbackService.isValidScore(5)).isTrue();
        assertThat(SatisfactionFeedbackService.isValidScore(6)).isFalse();
        assertThat(SatisfactionFeedbackService.isValidScore(-1)).isFalse();
    }
}
