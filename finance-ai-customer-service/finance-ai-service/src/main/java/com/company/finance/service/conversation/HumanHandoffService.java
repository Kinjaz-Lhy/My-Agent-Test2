package com.company.finance.service.conversation;

import com.company.finance.common.dto.ChatStreamResponse;
import com.company.finance.common.enums.SessionStatus;
import com.company.finance.domain.entity.Session;
import com.company.finance.infrastructure.mapper.SessionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * 人工坐席转接等待服务
 * <p>
 * 当会话处于 TRANSFERRED 状态时，每 30 秒通过 SSE 推送等待状态消息，
 * 告知员工正在等待人工坐席接入。当会话状态不再是 TRANSFERRED 时停止推送。
 * </p>
 *
 * @see <a href="需求 4.6">等待人工坐席接入状态更新</a>
 */
@Service
public class HumanHandoffService {

    private static final Logger log = LoggerFactory.getLogger(HumanHandoffService.class);

    /** 等待状态推送间隔（秒） */
    static final long WAITING_INTERVAL_SECONDS = 30;

    /** SSE 事件类型标识 */
    static final String EVENT_TYPE_WAITING = "waiting_status";

    private final SessionMapper sessionMapper;

    public HumanHandoffService(SessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
    }

    /**
     * 生成等待人工坐席接入的 SSE 事件流
     * <p>
     * 每 30 秒发送一条等待状态消息，直到会话状态不再是 TRANSFERRED。
     * </p>
     *
     * @param sessionId 会话 ID
     * @return SSE 事件流，会话非 TRANSFERRED 状态时自动完成
     */
    public Flux<ServerSentEvent<ChatStreamResponse>> streamWaitingStatus(String sessionId) {
        return Flux.interval(Duration.ofSeconds(WAITING_INTERVAL_SECONDS))
                .takeWhile(tick -> isSessionTransferred(sessionId))
                .map(tick -> {
                    long waitMinutes = (tick + 1) * WAITING_INTERVAL_SECONDS / 60;
                    String message = buildWaitingMessage(tick + 1, waitMinutes);
                    log.debug("推送等待状态: sessionId={}, tick={}", sessionId, tick + 1);

                    ChatStreamResponse response = ChatStreamResponse.builder()
                            .sessionId(sessionId)
                            .content(message)
                            .done(false)
                            .build();

                    return ServerSentEvent.<ChatStreamResponse>builder()
                            .id(sessionId)
                            .event(EVENT_TYPE_WAITING)
                            .data(response)
                            .build();
                });
    }

    /**
     * 检查会话是否仍处于 TRANSFERRED 状态
     */
    boolean isSessionTransferred(String sessionId) {
        try {
            Session session = sessionMapper.selectById(sessionId);
            return session != null && session.getStatus() == SessionStatus.TRANSFERRED;
        } catch (Exception e) {
            log.warn("查询会话状态失败: sessionId={}", sessionId, e);
            return false;
        }
    }

    /**
     * 构建等待提示消息
     */
    private String buildWaitingMessage(long tickCount, long waitMinutes) {
        if (waitMinutes < 1) {
            return "正在为您转接人工客服，请稍候...";
        }
        return "正在为您转接人工客服，已等待约 " + waitMinutes + " 分钟，请耐心等待...";
    }
}
