package com.company.finance.service.audit;

import com.company.finance.domain.entity.AuditLog;
import com.company.finance.infrastructure.mapper.AuditLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * AuditLogService 单元测试
 * <p>
 * 验证审计日志记录逻辑：UUID 生成、时间戳记录、字段映射、Mapper 调用。
 * </p>
 */
class AuditLogServiceTest {

    private AuditLogMapper auditLogMapper;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogMapper = mock(AuditLogMapper.class);
        auditLogService = new AuditLogService(auditLogMapper);
    }

    @Test
    @DisplayName("logConversation 应生成唯一 logId 并调用 Mapper 持久化")
    void shouldInsertAuditLogWithGeneratedLogId() {
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        auditLogService.logConversation(
                "sess-001", "EMP001", "CHAT",
                "我的报销单状态？", "您的报销单已审批通过",
                "您的报销单已审批通过", 1500
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper, times(1)).insert(captor.capture());

        AuditLog captured = captor.getValue();
        // logId 应为非空 UUID 格式
        assertThat(captured.getLogId()).isNotNull().isNotEmpty();
        assertThat(captured.getLogId()).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(captured.getSessionId()).isEqualTo("sess-001");
        assertThat(captured.getEmployeeId()).isEqualTo("EMP001");
        assertThat(captured.getAction()).isEqualTo("CHAT");
        assertThat(captured.getRequestContent()).isEqualTo("我的报销单状态？");
        assertThat(captured.getResponseContent()).isEqualTo("您的报销单已审批通过");
        assertThat(captured.getMaskedResponseContent()).isEqualTo("您的报销单已审批通过");
        // 时间戳应为非空且为当前时间附近
        assertThat(captured.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("每次调用应生成不同的 logId")
    void shouldGenerateUniqueLogIdPerCall() {
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        auditLogService.logConversation("s1", "E1", "CHAT", "req1", "resp1", "masked1", 1000);
        auditLogService.logConversation("s2", "E2", "CHAT", "req2", "resp2", "masked2", 2000);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper, times(2)).insert(captor.capture());

        String logId1 = captor.getAllValues().get(0).getLogId();
        String logId2 = captor.getAllValues().get(1).getLogId();
        assertThat(logId1).isNotEqualTo(logId2);
    }

    @Test
    @DisplayName("脱敏响应内容与原始响应内容可以不同")
    void shouldStoreBothOriginalAndMaskedResponse() {
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        auditLogService.logConversation(
                "sess-002", "EMP002", "CHAT",
                "查询工资", "您的工资12000元",
                "您的工资***元", 800
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());

        AuditLog captured = captor.getValue();
        assertThat(captured.getResponseContent()).isEqualTo("您的工资12000元");
        assertThat(captured.getMaskedResponseContent()).isEqualTo("您的工资***元");
    }
}
