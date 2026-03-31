-- ============================================================
-- V2: 会话表增加 title（重命名）和 pinned（置顶）字段
-- ============================================================

ALTER TABLE t_session
    ADD COLUMN title VARCHAR(200) COMMENT '会话标题（用户自定义）' AFTER department_id,
    ADD COLUMN pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶: 0=否, 1=是' AFTER title,
    ADD COLUMN pinned_at DATETIME COMMENT '置顶时间' AFTER pinned;

CREATE INDEX idx_pinned ON t_session (pinned, pinned_at DESC);
