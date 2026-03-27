package com.company.finance.service.satisfaction;

import com.company.finance.domain.entity.SatisfactionFeedback;
import com.company.finance.infrastructure.mapper.SatisfactionFeedbackMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 满意度评价服务
 * <p>
 * 会话结束时触发满意度评价请求，收集 1-5 分评分和文字反馈，
 * 并持久化到 t_satisfaction_feedback 表。
 * </p>
 *
 * @see <a href="需求 6.3">满意度评价</a>
 */
@Service
public class SatisfactionFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(SatisfactionFeedbackService.class);

    /** 最低评分 */
    static final int MIN_SCORE = 1;

    /** 最高评分 */
    static final int MAX_SCORE = 5;

    private final SatisfactionFeedbackMapper satisfactionFeedbackMapper;

    public SatisfactionFeedbackService(SatisfactionFeedbackMapper satisfactionFeedbackMapper) {
        this.satisfactionFeedbackMapper = satisfactionFeedbackMapper;
    }

    /**
     * 提交满意度评价
     * <p>
     * 验证评分范围（1-5），生成唯一 feedbackId，记录当前时间戳并持久化。
     * </p>
     *
     * @param sessionId  会话 ID
     * @param employeeId 员工 ID
     * @param score      评分（1-5）
     * @param comment    文字反馈（可选，可为 null）
     * @return 创建的满意度反馈实体
     * @throws IllegalArgumentException 评分不在 1-5 范围内时抛出
     */
    public SatisfactionFeedback submitFeedback(String sessionId,
                                                String employeeId,
                                                int score,
                                                String comment) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new IllegalArgumentException(
                    "评分必须在 " + MIN_SCORE + " 到 " + MAX_SCORE + " 之间，当前值: " + score);
        }

        SatisfactionFeedback feedback = SatisfactionFeedback.builder()
                .feedbackId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .employeeId(employeeId)
                .score(score)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();

        satisfactionFeedbackMapper.insert(feedback);
        log.info("满意度评价已提交: sessionId={}, score={}", sessionId, score);
        return feedback;
    }

    /**
     * 检查指定会话是否已存在满意度反馈
     *
     * @param sessionId 会话 ID
     * @return 如果已存在反馈返回 true，否则返回 false
     */
    public boolean feedbackExists(String sessionId) {
        return satisfactionFeedbackMapper.selectBySessionId(sessionId) != null;
    }

    /**
     * 查询指定日期范围内的平均满意度评分
     *
     * @param start 起始日期
     * @param end   结束日期
     * @return 平均评分，无数据时返回 null
     */
    public Double getAverageScore(LocalDate start, LocalDate end) {
        return satisfactionFeedbackMapper.selectAvgScoreByDateRange(start, end);
    }

    /**
     * 判断评分是否有效
     *
     * @param score 评分
     * @return 是否在有效范围内
     */
    public static boolean isValidScore(int score) {
        return score >= MIN_SCORE && score <= MAX_SCORE;
    }
}
