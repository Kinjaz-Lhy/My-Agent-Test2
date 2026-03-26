package com.company.finance.domain.repository;

import com.company.finance.domain.entity.ChatMessage;

import java.util.List;

/**
 * 对话消息仓储接口
 * <p>
 * 定义对话消息的持久化操作，由基础设施层实现。
 * </p>
 */
public interface ChatMessageRepository {

    /**
     * 保存消息
     *
     * @param message 消息实体
     */
    void save(ChatMessage message);

    /**
     * 根据会话 ID 查询消息列表（按时间戳升序）
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<ChatMessage> findBySessionId(String sessionId);
}
