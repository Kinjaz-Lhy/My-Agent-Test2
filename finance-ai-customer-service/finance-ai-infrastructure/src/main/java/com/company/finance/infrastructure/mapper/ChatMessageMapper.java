package com.company.finance.infrastructure.mapper;

import com.company.finance.domain.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对话消息 Mapper 接口
 * <p>
 * 提供对话消息的插入和按会话查询操作。
 * metadata 字段（Map 类型）在数据库中以 metadata_json（TEXT）形式存储，
 * Mapper 层以 String 形式处理 JSON，序列化/反序列化由上层服务处理。
 * </p>
 */
@Mapper
public interface ChatMessageMapper {

    /**
     * 插入对话消息
     *
     * @param message 消息实体
     * @param metadataJson 元数据 JSON 字符串
     * @return 影响行数
     */
    int insert(@Param("message") ChatMessage message, @Param("metadataJson") String metadataJson);

    /**
     * 根据会话 ID 查询消息列表（按时间戳升序）
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<ChatMessage> selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根据会话 ID 删除所有消息
     *
     * @param sessionId 会话 ID
     * @return 影响行数
     */
    int deleteBySessionId(@Param("sessionId") String sessionId);
}
