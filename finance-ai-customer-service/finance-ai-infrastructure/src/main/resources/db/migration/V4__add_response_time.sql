-- 审计日志表增加响应耗时字段
ALTER TABLE t_audit_log ADD COLUMN response_time_ms BIGINT DEFAULT 0 COMMENT '响应耗时（毫秒）' AFTER masked_response_content;
