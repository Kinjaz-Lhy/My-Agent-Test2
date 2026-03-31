-- -----------------------------------------------------------
-- 知识条目分类表：支持动态管理知识分类
-- -----------------------------------------------------------
CREATE TABLE t_knowledge_category (
    category_id VARCHAR(64) PRIMARY KEY COMMENT '分类唯一标识',
    code VARCHAR(64) NOT NULL UNIQUE COMMENT '分类编码，如 expense-policy',
    name VARCHAR(64) NOT NULL COMMENT '分类名称，如 报销制度',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识条目分类表';

-- 初始化默认分类
INSERT INTO t_knowledge_category (category_id, code, name, sort_order) VALUES
('cat-001', 'expense-policy', '报销制度', 1),
('cat-002', 'tax-policy', '税务政策', 2),
('cat-003', 'approval-flow', '审批流程', 3),
('cat-004', 'invoice-guide', '发票指南', 4);
