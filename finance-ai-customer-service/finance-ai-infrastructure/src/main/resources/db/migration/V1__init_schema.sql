-- ============================================================
-- 财务共享智能 AI 客服系统 - 数据库初始化脚本 (MySQL)
-- 版本: V1
-- 描述: 创建所有核心业务表
-- ============================================================

-- -----------------------------------------------------------
-- 会话表：记录员工与智能客服之间的对话会话
-- -----------------------------------------------------------
CREATE TABLE t_session (
    session_id VARCHAR(64) PRIMARY KEY COMMENT '会话唯一标识',
    employee_id VARCHAR(64) NOT NULL COMMENT '员工ID',
    department_id VARCHAR(64) COMMENT '部门ID',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态: ACTIVE/TRANSFERRED/CLOSED',
    context_json TEXT COMMENT '上下文元数据（JSON格式）',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    closed_at DATETIME COMMENT '关闭时间',
    INDEX idx_employee_id (employee_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- -----------------------------------------------------------
-- 对话消息表：记录会话中的每条消息
-- -----------------------------------------------------------
CREATE TABLE t_chat_message (
    message_id VARCHAR(64) PRIMARY KEY COMMENT '消息唯一标识',
    session_id VARCHAR(64) NOT NULL COMMENT '所属会话ID',
    role VARCHAR(20) NOT NULL COMMENT '消息角色: USER/ASSISTANT/SYSTEM/TOOL',
    content TEXT NOT NULL COMMENT '消息内容',
    intent VARCHAR(64) COMMENT '识别的用户意图',
    metadata_json TEXT COMMENT '元数据（JSON格式，如工具调用结果）',
    timestamp DATETIME NOT NULL COMMENT '消息时间戳',
    INDEX idx_session_id (session_id),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';

-- -----------------------------------------------------------
-- 知识条目表：存储财务制度、政策、流程等知识内容
-- -----------------------------------------------------------
CREATE TABLE t_knowledge_entry (
    entry_id VARCHAR(64) PRIMARY KEY COMMENT '知识条目唯一标识',
    category VARCHAR(64) NOT NULL COMMENT '分类: 报销制度/税务政策/审批流程等',
    title VARCHAR(256) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '知识内容',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING_REVIEW/ACTIVE/ARCHIVED',
    created_by VARCHAR(64) COMMENT '创建人',
    reviewed_by VARCHAR(64) COMMENT '审核人',
    effective_at DATETIME COMMENT '生效时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_category_status (category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识条目表';

-- -----------------------------------------------------------
-- 审计日志表：记录所有对话交互的完整审计日志
-- -----------------------------------------------------------
CREATE TABLE t_audit_log (
    log_id VARCHAR(64) PRIMARY KEY COMMENT '日志唯一标识',
    session_id VARCHAR(64) COMMENT '关联会话ID',
    employee_id VARCHAR(64) NOT NULL COMMENT '员工ID',
    action VARCHAR(32) NOT NULL COMMENT '操作类型: CHAT/TOOL_CALL/HANDOFF/LOGIN',
    request_content TEXT COMMENT '请求内容',
    response_content TEXT COMMENT '响应内容',
    masked_response_content TEXT COMMENT '脱敏后的响应内容',
    timestamp DATETIME NOT NULL COMMENT '操作时间戳',
    INDEX idx_employee_id (employee_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- -----------------------------------------------------------
-- 运营指标表：按日汇总的运营统计数据
-- -----------------------------------------------------------
CREATE TABLE t_operation_metrics (
    metric_id VARCHAR(64) PRIMARY KEY COMMENT '指标唯一标识',
    metric_date DATE NOT NULL UNIQUE COMMENT '统计日期（唯一）',
    total_sessions BIGINT DEFAULT 0 COMMENT '总会话数',
    self_resolved_sessions BIGINT DEFAULT 0 COMMENT '自助解决会话数',
    human_handoff_sessions BIGINT DEFAULT 0 COMMENT '人工转接会话数',
    avg_response_time_ms DOUBLE DEFAULT 0 COMMENT '平均响应时间（毫秒）',
    satisfaction_score DOUBLE DEFAULT 0 COMMENT '平均满意度评分',
    hot_topics_json TEXT COMMENT '热点问题统计（JSON格式）',
    INDEX idx_metric_date (metric_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营指标表';

-- -----------------------------------------------------------
-- 满意度反馈表：记录员工对会话的满意度评价
-- -----------------------------------------------------------
CREATE TABLE t_satisfaction_feedback (
    feedback_id VARCHAR(64) PRIMARY KEY COMMENT '反馈唯一标识',
    session_id VARCHAR(64) NOT NULL COMMENT '关联会话ID',
    employee_id VARCHAR(64) NOT NULL COMMENT '员工ID',
    score INT NOT NULL COMMENT '满意度评分（1-5分）',
    comment TEXT COMMENT '文字反馈（可选）',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满意度反馈表';
