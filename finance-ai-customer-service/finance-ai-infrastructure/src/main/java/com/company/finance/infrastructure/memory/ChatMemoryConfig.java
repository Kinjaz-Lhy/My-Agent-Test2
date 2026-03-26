package com.company.finance.infrastructure.memory;

import kd.ai.nova.core.memory.chat.ConversationChatMemory;
import kd.ai.nova.core.memory.ChatMemory;
import kd.ai.nova.core.store.memory.chat.RelationalDBMemoryStore;
import kd.ai.nova.chat.advisor.MessageChatMemoryAdvisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * 对话记忆配置工厂
 * <p>
 * 基于 AI-Nova 框架的 RelationalDBMemoryStore 实现数据库持久化存储，
 * 配合 ConversationChatMemory 实现生产环境的多轮对话记忆。
 * </p>
 * <p>
 * 配置参数：
 * <ul>
 *   <li>maxMessages = 100（支持 50 轮对话，每轮 user + assistant = 2 条）</li>
 *   <li>存储层使用 RelationalDBMemoryStore（MySQL 持久化）</li>
 * </ul>
 * </p>
 */
public class ChatMemoryConfig {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryConfig.class);

    /** 最大消息数量：100 条（支持 50 轮对话） */
    public static final int MAX_MESSAGES = 100;

    private ChatMemoryConfig() {
    }

    /**
     * 创建基于数据库持久化的对话记忆
     *
     * @param dataSource 数据源
     * @return ChatMemory 实例
     */
    public static ChatMemory createChatMemory(DataSource dataSource) {
        log.info("初始化对话记忆：maxMessages={}, 存储=RelationalDBMemoryStore", MAX_MESSAGES);

        RelationalDBMemoryStore store = RelationalDBMemoryStore.builder()
                .dataSource(dataSource)
                .build();

        return ConversationChatMemory.builder()
                .maxMessages(MAX_MESSAGES)
                .chatMemoryStore(store)
                .build();
    }

    /**
     * 创建对话记忆 Advisor
     *
     * @param dataSource 数据源
     * @return MessageChatMemoryAdvisor 实例
     */
    public static MessageChatMemoryAdvisor createMemoryAdvisor(DataSource dataSource) {
        ChatMemory chatMemory = createChatMemory(dataSource);
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}
