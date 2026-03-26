package com.company.finance.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 账户锁定服务。
 * <p>
 * 记录连续认证失败次数，达到 5 次后锁定账户 30 分钟，并通知系统管理员。
 * 锁定过期后自动解锁。线程安全实现。
 * </p>
 *
 * @see <a href="需求 5.7">连续 5 次认证失败锁定账户 30 分钟并通知管理员</a>
 */
@Service
public class AccountLockService {

    private static final Logger log = LoggerFactory.getLogger(AccountLockService.class);

    /** 最大连续失败次数 */
    static final int MAX_FAILURES = 5;

    /** 锁定时长（分钟） */
    static final int LOCK_DURATION_MINUTES = 30;

    /** 存储每个用户的锁定信息，key 为 employeeId */
    private final ConcurrentHashMap<String, LockInfo> lockMap = new ConcurrentHashMap<>();

    /**
     * 记录一次认证失败。
     * <p>
     * 如果连续失败次数达到 {@value MAX_FAILURES} 次，锁定账户 {@value LOCK_DURATION_MINUTES} 分钟，
     * 并记录警告日志通知管理员。
     * </p>
     *
     * @param employeeId 员工 ID
     * @return 当前连续失败次数
     */
    public int recordFailure(String employeeId) {
        LockInfo lockInfo = lockMap.compute(employeeId, (key, existing) -> {
            if (existing == null) {
                return new LockInfo(1, null);
            }
            // 如果已锁定且未过期，不再累加失败次数
            if (existing.isLocked()) {
                return existing;
            }
            // 如果锁定已过期，重新开始计数
            if (existing.lockedUntil != null && !existing.isLocked()) {
                return new LockInfo(1, null);
            }
            int newCount = existing.failureCount + 1;
            LocalDateTime lockedUntil = null;
            if (newCount >= MAX_FAILURES) {
                lockedUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
                log.warn("[安全告警] 员工 {} 连续认证失败 {} 次，账户已锁定至 {}，请管理员关注。",
                        employeeId, newCount, lockedUntil);
            }
            return new LockInfo(newCount, lockedUntil);
        });
        return lockInfo.failureCount;
    }

    /**
     * 检查账户是否被锁定。
     * <p>
     * 如果锁定时间已过期，自动解锁并清除锁定信息。
     * </p>
     *
     * @param employeeId 员工 ID
     * @return 如果账户当前被锁定返回 true，否则返回 false
     */
    public boolean isLocked(String employeeId) {
        LockInfo lockInfo = lockMap.get(employeeId);
        if (lockInfo == null) {
            return false;
        }
        if (lockInfo.lockedUntil == null) {
            return false;
        }
        // 锁定已过期，自动解锁
        if (LocalDateTime.now().isAfter(lockInfo.lockedUntil)) {
            lockMap.remove(employeeId);
            return false;
        }
        return true;
    }

    /**
     * 认证成功后重置失败计数。
     *
     * @param employeeId 员工 ID
     */
    public void resetFailures(String employeeId) {
        lockMap.remove(employeeId);
    }

    /**
     * 锁定信息内部类，存储失败次数和锁定截止时间。
     */
    static class LockInfo {

        /** 连续失败次数 */
        final int failureCount;

        /** 锁定截止时间，为 null 表示未锁定 */
        final LocalDateTime lockedUntil;

        LockInfo(int failureCount, LocalDateTime lockedUntil) {
            this.failureCount = failureCount;
            this.lockedUntil = lockedUntil;
        }

        /**
         * 判断当前是否处于锁定状态。
         *
         * @return 如果锁定截止时间存在且未过期返回 true
         */
        boolean isLocked() {
            return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
        }
    }
}
