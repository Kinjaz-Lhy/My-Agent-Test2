package com.company.finance.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AccountLockService 单元测试。
 * 验证账户锁定机制的核心逻辑：失败计数、锁定触发、自动解锁、重置。
 */
class AccountLockServiceTest {

    private AccountLockService service;

    @BeforeEach
    void setUp() {
        service = new AccountLockService();
    }

    @Test
    @DisplayName("首次失败应返回失败次数 1")
    void recordFailure_firstTime_returnsOne() {
        int count = service.recordFailure("EMP001");
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("连续失败应累加计数")
    void recordFailure_consecutive_incrementsCount() {
        for (int i = 1; i <= 4; i++) {
            int count = service.recordFailure("EMP001");
            assertThat(count).isEqualTo(i);
        }
    }

    @Test
    @DisplayName("连续 5 次失败后账户应被锁定")
    void recordFailure_fiveFailures_locksAccount() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("EMP001");
        }
        assertThat(service.isLocked("EMP001")).isTrue();
    }

    @Test
    @DisplayName("未达到 5 次失败时账户不应被锁定")
    void isLocked_belowThreshold_returnsFalse() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("EMP001");
        }
        assertThat(service.isLocked("EMP001")).isFalse();
    }

    @Test
    @DisplayName("不同用户的失败计数应独立")
    void recordFailure_differentUsers_independentCounts() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("EMP001");
        }
        service.recordFailure("EMP002");

        assertThat(service.isLocked("EMP001")).isTrue();
        assertThat(service.isLocked("EMP002")).isFalse();
    }

    @Test
    @DisplayName("认证成功后应重置失败计数")
    void resetFailures_clearsCount() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure("EMP001");
        }
        service.resetFailures("EMP001");

        // 重置后重新计数，1 次失败不应锁定
        service.recordFailure("EMP001");
        assertThat(service.isLocked("EMP001")).isFalse();
    }

    @Test
    @DisplayName("未记录过失败的用户不应被锁定")
    void isLocked_unknownUser_returnsFalse() {
        assertThat(service.isLocked("UNKNOWN")).isFalse();
    }

    @Test
    @DisplayName("锁定过期后应自动解锁")
    void isLocked_afterExpiry_returnsFalse() throws Exception {
        // 先锁定账户
        for (int i = 0; i < 5; i++) {
            service.recordFailure("EMP001");
        }
        assertThat(service.isLocked("EMP001")).isTrue();

        // 通过反射将锁定时间设置为过去，模拟过期
        Field lockMapField = AccountLockService.class.getDeclaredField("lockMap");
        lockMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, AccountLockService.LockInfo> lockMap =
                (ConcurrentHashMap<String, AccountLockService.LockInfo>) lockMapField.get(service);

        // 替换为已过期的 LockInfo
        lockMap.put("EMP001", new AccountLockService.LockInfo(5, LocalDateTime.now().minusMinutes(1)));

        assertThat(service.isLocked("EMP001")).isFalse();
    }

    @Test
    @DisplayName("锁定期间继续调用 recordFailure 不应增加计数")
    void recordFailure_whileLocked_doesNotIncrement() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("EMP001");
        }
        // 锁定后再次调用
        int count = service.recordFailure("EMP001");
        assertThat(count).isEqualTo(5);
        assertThat(service.isLocked("EMP001")).isTrue();
    }

    @Test
    @DisplayName("锁定过期后重新失败应从 1 开始计数")
    void recordFailure_afterExpiry_restartsCount() throws Exception {
        // 先锁定
        for (int i = 0; i < 5; i++) {
            service.recordFailure("EMP001");
        }

        // 模拟锁定过期
        Field lockMapField = AccountLockService.class.getDeclaredField("lockMap");
        lockMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, AccountLockService.LockInfo> lockMap =
                (ConcurrentHashMap<String, AccountLockService.LockInfo>) lockMapField.get(service);
        lockMap.put("EMP001", new AccountLockService.LockInfo(5, LocalDateTime.now().minusMinutes(1)));

        // 过期后再次失败，应从 1 开始
        int count = service.recordFailure("EMP001");
        assertThat(count).isEqualTo(1);
        assertThat(service.isLocked("EMP001")).isFalse();
    }

    @Test
    @DisplayName("第 5 次失败应返回计数 5")
    void recordFailure_fifthFailure_returnsFive() {
        int count = 0;
        for (int i = 0; i < 5; i++) {
            count = service.recordFailure("EMP001");
        }
        assertThat(count).isEqualTo(5);
    }
}
