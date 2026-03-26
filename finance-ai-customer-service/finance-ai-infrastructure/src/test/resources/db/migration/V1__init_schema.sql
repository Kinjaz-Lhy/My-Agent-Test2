-- ============================================================
-- 财务共享智能 AI 客服系统 - 数据库初始化脚本 (H2 测试环境)
-- 版本: V1
-- 描述: 创建所有核心业务表（H2 兼容语法）
-- ============================================================

-- -----------------------------------------------------------
-- 会话表：记录员工与智能客服之间的对话会话
-- -----------------------------------------------------------
CREATE TABLE t_session (
    session_id VARCHAR(64) PRIMARY KEY,          -- 会话唯一标识
    employee_id VARCHAR(64) NOT NULL,            -- 员工ID
    department_id VARCHAR(64),                   -- 部门ID
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',-- 会话状态: ACTIVE/TRANSFERRED/CLOSED
    context_json TEXT,                           -- 上下文元数据（JSON格式）
    created_at DATETIME NOT NULL,                -- 创建时间
    updated_at DATETIME NOT NULL,                -- 更新时间
    closed_at DATETIME                           -- 关闭时间
);
CREATE INDEX idx_session_employee_id ON t_session(employee_id);
CREATE INDEX idx_session_status ON t_session(status);
CREATE INDEX idx_session_created_at ON t_session(created_at);

-- -----------------------------------------------------------
-- 对话消息表：记录会话中的每条消息
-- -----------------------------------------------------------
CREATE TABLE t_chat_message (
    message_id VARCHAR(64) PRIMARY KEY,          -- 消息唯一标识
    session_id VARCHAR(64) NOT NULL,             -- 所属会话ID
    role VARCHAR(20) NOT NULL,                   -- 消息角色: USER/ASSISTANT/SYSTEM/TOOL
    content TEXT NOT NULL,                       -- 消息内容
    intent VARCHAR(64),                          -- 识别的用户意图
    metadata_json TEXT,                          -- 元数据（JSON格式，如工具调用结果）
    timestamp DATETIME NOT NULL                  -- 消息时间戳
);
CREATE INDEX idx_message_session_id ON t_chat_message(session_id);
CREATE INDEX idx_message_timestamp ON t_chat_message(timestamp);

-- -----------------------------------------------------------
-- 知识条目表：存储财务制度、政策、流程等知识内容
-- -----------------------------------------------------------
CREATE TABLE t_knowledge_entry (
    entry_id VARCHAR(64) PRIMARY KEY,            -- 知识条目唯一标识
    category VARCHAR(64) NOT NULL,               -- 分类: 报销制度/税务政策/审批流程等
    title VARCHAR(256) NOT NULL,                 -- 标题
    content TEXT NOT NULL,                       -- 知识内容
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- 状态: DRAFT/PENDING_REVIEW/ACTIVE/ARCHIVED
    created_by VARCHAR(64),                      -- 创建人
    reviewed_by VARCHAR(64),                     -- 审核人
    effective_at DATETIME,                       -- 生效时间
    created_at DATETIME NOT NULL,                -- 创建时间
    updated_at DATETIME NOT NULL                 -- 更新时间
);
CREATE INDEX idx_knowledge_category_status ON t_knowledge_entry(category, status);

-- -----------------------------------------------------------
-- 审计日志表：记录所有对话交互的完整审计日志
-- -----------------------------------------------------------
CREATE TABLE t_audit_log (
    log_id VARCHAR(64) PRIMARY KEY,              -- 日志唯一标识
    session_id VARCHAR(64),                      -- 关联会话ID
    employee_id VARCHAR(64) NOT NULL,            -- 员工ID
    action VARCHAR(32) NOT NULL,                 -- 操作类型: CHAT/TOOL_CALL/HANDOFF/LOGIN
    request_content TEXT,                        -- 请求内容
    response_content TEXT,                       -- 响应内容
    masked_response_content TEXT,                -- 脱敏后的响应内容
    timestamp DATETIME NOT NULL                  -- 操作时间戳
);
CREATE INDEX idx_audit_employee_id ON t_audit_log(employee_id);
CREATE INDEX idx_audit_timestamp ON t_audit_log(timestamp);
CREATE INDEX idx_audit_session_id ON t_audit_log(session_id);

-- -----------------------------------------------------------
-- 运营指标表：按日汇总的运营统计数据
-- -----------------------------------------------------------
CREATE TABLE t_operation_metrics (
    metric_id VARCHAR(64) PRIMARY KEY,           -- 指标唯一标识
    metric_date DATE NOT NULL UNIQUE,            -- 统计日期（唯一）
    total_sessions BIGINT DEFAULT 0,             -- 总会话数
    self_resolved_sessions BIGINT DEFAULT 0,     -- 自助解决会话数
    human_handoff_sessions BIGINT DEFAULT 0,     -- 人工转接会话数
    avg_response_time_ms DOUBLE DEFAULT 0,       -- 平均响应时间（毫秒）
    satisfaction_score DOUBLE DEFAULT 0,         -- 平均满意度评分
    hot_topics_json TEXT                         -- 热点问题统计（JSON格式）
);
CREATE INDEX idx_metrics_metric_date ON t_operation_metrics(metric_date);

-- -----------------------------------------------------------
-- 满意度反馈表：记录员工对会话的满意度评价
-- -----------------------------------------------------------
CREATE TABLE t_satisfaction_feedback (
    feedback_id VARCHAR(64) PRIMARY KEY,         -- 反馈唯一标识
    session_id VARCHAR(64) NOT NULL,             -- 关联会话ID
    employee_id VARCHAR(64) NOT NULL,            -- 员工ID
    score INT NOT NULL,                          -- 满意度评分（1-5分）
    comment TEXT,                                -- 文字反馈（可选）
    created_at DATETIME NOT NULL                 -- 创建时间
);
CREATE INDEX idx_feedback_session_id ON t_satisfaction_feedback(session_id);
CREATE INDEX idx_feedback_created_at ON t_satisfaction_feedback(created_at);
