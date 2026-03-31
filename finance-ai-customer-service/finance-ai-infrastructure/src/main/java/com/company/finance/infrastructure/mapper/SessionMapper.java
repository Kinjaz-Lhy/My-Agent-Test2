package com.company.finance.infrastructure.mapper;

import com.company.finance.domain.entity.Session;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会话 Mapper 接口
 * <p>
 * 提供会话的增删改查操作。
 * context 字段（Map 类型）在数据库中以 context_json（TEXT）形式存储，
 * Mapper 层以 String 形式处理 JSON，序列化/反序列化由上层服务处理。
 * </p>
 */
@Mapper
public interface SessionMapper {

    /**
     * 插入会话记录
     *
     * @param session 会话实体（context 字段需由上层转为 contextJson 字符串）
     * @param contextJson 上下文 JSON 字符串
     * @return 影响行数
     */
    int insert(@Param("session") Session session, @Param("contextJson") String contextJson);

    /**
     * 根据会话 ID 查询会话
     *
     * @param sessionId 会话 ID
     * @return 会话实体（contextJson 以 String 形式返回，需上层反序列化）
     */
    Session selectById(@Param("sessionId") String sessionId);

    /**
     * 根据员工 ID 查询会话列表（按创建时间降序）
     *
     * @param employeeId 员工 ID
     * @return 会话列表
     */
    List<Session> selectByEmployeeId(@Param("employeeId") String employeeId);

    /**
     * 更新会话状态
     *
     * @param sessionId 会话 ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatus(@Param("sessionId") String sessionId, @Param("status") String status);

    /**
     * 更新会话标题
     *
     * @param sessionId 会话 ID
     * @param title 新标题
     * @return 影响行数
     */
    int updateTitle(@Param("sessionId") String sessionId, @Param("title") String title);

    /**
     * 更新会话置顶状态
     *
     * @param sessionId 会话 ID
     * @param pinned 是否置顶
     * @param pinnedAt 置顶时间（取消置顶时为 null）
     * @return 影响行数
     */
    int updatePinned(@Param("sessionId") String sessionId, @Param("pinned") boolean pinned,
                     @Param("pinnedAt") java.time.LocalDateTime pinnedAt);

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     * @return 影响行数
     */
    int deleteById(@Param("sessionId") String sessionId);
}
