package com.company.finance.service.advisor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HumanHandoffAdvisor 单元测试。
 * <p>
 * 验证未满足需求检测逻辑和计数管理。
 * Advisor 链的实际调用依赖 AI-Nova 框架运行时，
 * 此处仅验证核心业务逻辑。
 * </p>
 */
class HumanHandoffAdvisorTest {

    private HumanHandoffAdvisor advisor;

    @BeforeEach
    void setUp() {
        advisor = new HumanHandoffAdvisor();
    }

    @Nested
    class UnsatisfiedResponseDetection {

        @Test
        void detectsApologyKeyword() {
            assertThat(advisor.isUnsatisfiedResponse("抱歉，我无法理解您的问题")).isTrue();
        }

        @Test
        void detectsUnableToProcessKeyword() {
            assertThat(advisor.isUnsatisfiedResponse("这个问题无法处理，请联系管理员")).isTrue();
        }

        @Test
        void detectsUncertainKeyword() {
            assertThat(advisor.isUnsatisfiedResponse("我不确定这个问题的答案")).isTrue();
        }

        @Test
        void detectsSuggestHumanKeyword() {
            assertThat(advisor.isUnsatisfiedResponse("建议联系人工客服获取帮助")).isTrue();
        }

        @Test
        void detectsNotFoundKeyword() {
            assertThat(advisor.isUnsatisfiedResponse("暂未找到相关信息")).isTrue();
        }

        @Test
        void normalResponseIsNotUnsatisfied() {
            assertThat(advisor.isUnsatisfiedResponse("您的报销单 EXP-001 当前状态为已审批")).isFalse();
        }

        @Test
        void nullResponseIsNotUnsatisfied() {
            assertThat(advisor.isUnsatisfiedResponse(null)).isFalse();
        }

        @Test
        void emptyResponseIsNotUnsatisfied() {
            assertThat(advisor.isUnsatisfiedResponse("")).isFalse();
        }
    }

    @Nested
    class CountManagement {

        @Test
        void initialCountIsZero() {
            assertThat(advisor.getUnsatisfiedCount("session-1")).isEqualTo(0);
        }

        @Test
        void resetClearsCount() {
            // Simulate some state by directly testing reset
            advisor.resetCount("session-1");
            assertThat(advisor.getUnsatisfiedCount("session-1")).isEqualTo(0);
        }
    }

    @Nested
    class ThresholdConfiguration {

        @Test
        void defaultThresholdIsThree() {
            assertThat(HumanHandoffAdvisor.DEFAULT_THRESHOLD).isEqualTo(3);
        }

        @Test
        void unsatisfiedKeywordsAreNotEmpty() {
            assertThat(HumanHandoffAdvisor.UNSATISFIED_KEYWORDS).isNotEmpty();
        }
    }
}
