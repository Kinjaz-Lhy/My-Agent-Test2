package com.company.finance.domain.repository;

import com.company.finance.common.enums.SessionStatus;
import com.company.finance.domain.entity.Session;

import java.util.List;
import java.util.Optional;

/**
 * 会话仓储接口
 * <p>
 * 定义会话实体的持久化操作，由基础设施层实现。
 * </p>
 */
public interface SessionRepository {

    /**
     * 保存会话
     *
     * @param session 会话实体
     */
    void save(Session session);

    /**
     * 根据会话 ID 查询
     *
     * @param sessionId 会话 ID
     * @return 会话实体（可能为空）
     */
    Optional<Session> findById(String sessionId);

    /**
     * 根据员工 ID 查询会话列表（按创建时间降序）
     *
     * @param employeeId 员工 ID
     * @return 会话列表
     */
    List<Session> findByEmployeeId(String employeeId);

    /**
     * 更新会话状态
     *
     * @param sessionId 会话 ID
     * @param status    目标状态
     */
    void updateStatus(String sessionId, SessionStatus status);
}
