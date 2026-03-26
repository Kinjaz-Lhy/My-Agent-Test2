package com.company.finance.domain.serializer;

import com.company.finance.common.enums.MessageRole;
import com.company.finance.common.enums.SessionStatus;
import com.company.finance.domain.entity.ChatMessage;
import com.company.finance.domain.entity.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SessionSerializer 单元测试
 */
class SessionSerializerTest {

    private SessionSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new SessionSerializer();
    }

    /**
     * 构建一个完整的测试 Session 对象
     */
    private Session buildTestSession() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("toolCall", "expenseQuery");
        metadata.put("expenseId", "EXP-2024-001");

        ChatMessage userMsg = ChatMessage.builder()
                .messageId("msg-001")
                .sessionId("sess-001")
                .role(MessageRole.USER)
                .content("我的报销单审批到哪一步了？")
                .intent("EXPENSE_STATUS_QUERY")
                .timestamp(LocalDateTime.of(2024, 1, 1, 10, 0, 0))
                .build();

        ChatMessage assistantMsg = ChatMessage.builder()
                .messageId("msg-002")
                .sessionId("sess-001")
                .role(MessageRole.ASSISTANT)
                .content("您的报销单 EXP-2024-001 当前状态为已审批")
                .metadata(metadata)
                .timestamp(LocalDateTime.of(2024, 1, 1, 10, 0, 2))
                .build();

        Map<String, Object> context = new HashMap<>();
        context.put("currentTopic", "expense");
        context.put("turnCount", 2);

        return Session.builder()
                .sessionId("sess-001")
                .employeeId("EMP001")
                .departmentId("DEPT-FIN")
                .status(SessionStatus.ACTIVE)
                .messages(Arrays.asList(userMsg, assistantMsg))
                .context(context)
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 1, 10, 0, 2))
                .build();
    }

    @Test
    @DisplayName("序列化：完整 Session 对象序列化为 JSON 字符串")
    void serialize_fullSession_returnsValidJson() throws Exception {
        Session session = buildTestSession();

        String json = serializer.serialize(session);

        // 验证是合法 JSON
        new ObjectMapper().readTree(json);

        // 验证关键字段存在
        assertThat(json).contains("sess-001");
        assertThat(json).contains("EMP001");
        assertThat(json).contains("ACTIVE");
        assertThat(json).contains("msg-001");
        assertThat(json).contains("USER");
        assertThat(json).contains("ASSISTANT");
    }

    @Test
    @DisplayName("序列化：LocalDateTime 序列化为字符串格式而非时间戳")
    void serialize_localDateTime_notTimestamp() {
        Session session = buildTestSession();

        String json = serializer.serialize(session);

        // 不应包含纯数字时间戳，应包含日期字符串
        assertThat(json).contains("2024-01-01");
        assertThat(json).doesNotContain("1704067200");
    }

    @Test
    @DisplayName("序列化：枚举序列化为字符串")
    void serialize_enums_asStrings() {
        Session session = buildTestSession();

        String json = serializer.serialize(session);

        assertThat(json).contains("\"ACTIVE\"");
        assertThat(json).contains("\"USER\"");
        assertThat(json).contains("\"ASSISTANT\"");
    }

    @Test
    @DisplayName("序列化：Map 类型字段正确序列化")
    void serialize_mapFields_correctlySerialized() {
        Session session = buildTestSession();

        String json = serializer.serialize(session);

        // context 字段
        assertThat(json).contains("currentTopic");
        assertThat(json).contains("expense");
        // metadata 字段
        assertThat(json).contains("toolCall");
        assertThat(json).contains("expenseQuery");
    }

    @Test
    @DisplayName("序列化：null 参数抛出 IllegalArgumentException")
    void serialize_nullSession_throwsException() {
        assertThatThrownBy(() -> serializer.serialize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("反序列化：JSON 字符串还原为 Session 对象")
    void deserialize_validJson_returnsSession() {
        Session original = buildTestSession();
        String json = serializer.serialize(original);

        Session deserialized = serializer.deserialize(json);

        assertThat(deserialized.getSessionId()).isEqualTo("sess-001");
        assertThat(deserialized.getEmployeeId()).isEqualTo("EMP001");
        assertThat(deserialized.getDepartmentId()).isEqualTo("DEPT-FIN");
        assertThat(deserialized.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(deserialized.getMessages()).hasSize(2);
        assertThat(deserialized.getContext()).containsKey("currentTopic");
    }

    @Test
    @DisplayName("反序列化：消息列表中的枚举和时间正确还原")
    void deserialize_messagesWithEnumsAndTime_correctlyRestored() {
        Session original = buildTestSession();
        String json = serializer.serialize(original);

        Session deserialized = serializer.deserialize(json);

        ChatMessage firstMsg = deserialized.getMessages().get(0);
        assertThat(firstMsg.getRole()).isEqualTo(MessageRole.USER);
        assertThat(firstMsg.getTimestamp()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0));

        ChatMessage secondMsg = deserialized.getMessages().get(1);
        assertThat(secondMsg.getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(secondMsg.getMetadata()).containsEntry("toolCall", "expenseQuery");
    }

    @Test
    @DisplayName("反序列化：空字符串抛出 IllegalArgumentException")
    void deserialize_emptyString_throwsException() {
        assertThatThrownBy(() -> serializer.deserialize(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("反序列化：null 参数抛出 IllegalArgumentException")
    void deserialize_null_throwsException() {
        assertThatThrownBy(() -> serializer.deserialize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("反序列化：非法 JSON 抛出 SessionSerializationException")
    void deserialize_invalidJson_throwsException() {
        assertThatThrownBy(() -> serializer.deserialize("{invalid json}"))
                .isInstanceOf(SessionSerializer.SessionSerializationException.class);
    }

    @Test
    @DisplayName("往返一致性：serialize → deserialize 后字段等价")
    void roundTrip_serializeDeserialize_fieldsEqual() {
        Session original = buildTestSession();

        String json = serializer.serialize(original);
        Session restored = serializer.deserialize(json);

        assertThat(restored.getSessionId()).isEqualTo(original.getSessionId());
        assertThat(restored.getEmployeeId()).isEqualTo(original.getEmployeeId());
        assertThat(restored.getDepartmentId()).isEqualTo(original.getDepartmentId());
        assertThat(restored.getStatus()).isEqualTo(original.getStatus());
        assertThat(restored.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(restored.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
        assertThat(restored.getClosedAt()).isEqualTo(original.getClosedAt());
        assertThat(restored.getMessages()).hasSameSizeAs(original.getMessages());
        assertThat(restored.getContext()).isEqualTo(original.getContext());
    }

    @Test
    @DisplayName("prettyPrint：输出包含缩进且为合法 JSON")
    void prettyPrint_returnsIndentedValidJson() throws Exception {
        Session session = buildTestSession();

        String pretty = serializer.prettyPrint(session);

        // 验证包含缩进（换行 + 空格）
        assertThat(pretty).contains("\n");
        assertThat(pretty).contains("  ");

        // 验证是合法 JSON
        new ObjectMapper().readTree(pretty);
    }

    @Test
    @DisplayName("prettyPrint：null 参数抛出 IllegalArgumentException")
    void prettyPrint_nullSession_throwsException() {
        assertThatThrownBy(() -> serializer.prettyPrint(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("序列化：closedAt 为 null 时不输出该字段")
    void serialize_nullClosedAt_fieldOmitted() {
        Session session = buildTestSession();
        session.setClosedAt(null);

        String json = serializer.serialize(session);

        assertThat(json).doesNotContain("closedAt");
    }

    @Test
    @DisplayName("序列化：空消息列表正确处理")
    void serialize_emptyMessages_handledCorrectly() {
        Session session = Session.builder()
                .sessionId("sess-empty")
                .employeeId("EMP002")
                .status(SessionStatus.CLOSED)
                .createdAt(LocalDateTime.of(2024, 6, 1, 8, 0, 0))
                .updatedAt(LocalDateTime.of(2024, 6, 1, 8, 0, 0))
                .closedAt(LocalDateTime.of(2024, 6, 1, 9, 0, 0))
                .build();

        String json = serializer.serialize(session);
        Session restored = serializer.deserialize(json);

        assertThat(restored.getSessionId()).isEqualTo("sess-empty");
        assertThat(restored.getMessages()).isEmpty();
        assertThat(restored.getStatus()).isEqualTo(SessionStatus.CLOSED);
    }
}
