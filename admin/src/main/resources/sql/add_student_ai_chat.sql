-- 学生端智能问答基础表和权限

CREATE TABLE IF NOT EXISTS ai_chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    student_code VARCHAR(64) NOT NULL COMMENT '学生编号',
    title VARCHAR(120) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / ARCHIVED / DELETED',
    message_count INT NOT NULL DEFAULT 0 COMMENT '消息数量',
    last_message_at DATETIME NOT NULL COMMENT '最后消息时间',
    summary MEDIUMTEXT NULL COMMENT '会话摘要',
    summary_token_count INT NOT NULL DEFAULT 0 COMMENT '摘要token数',
    compressed_until_message_id BIGINT NULL COMMENT '已压缩到的消息ID',
    archive_status VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT 'NONE / ARCHIVED / EXPIRED',
    archive_id BIGINT NULL COMMENT '归档记录ID',
    expire_at DATETIME NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_ai_chat_session_student_time (student_id, status, last_message_at),
    INDEX idx_ai_chat_session_student_code (student_code)
) COMMENT='学生端AI问答会话表';

CREATE TABLE IF NOT EXISTS ai_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    role VARCHAR(20) NOT NULL COMMENT 'user / assistant / system',
    content MEDIUMTEXT NOT NULL COMMENT '消息正文',
    content_type VARCHAR(20) NOT NULL DEFAULT 'markdown' COMMENT 'text / markdown',
    token_count INT NOT NULL DEFAULT 0 COMMENT 'token数',
    retrieval_id BIGINT NULL COMMENT '关联检索记录',
    source_refs_json JSON NULL COMMENT '回答引用来源',
    model_name VARCHAR(100) NULL COMMENT '使用的模型',
    finish_reason VARCHAR(40) NULL COMMENT 'stop / length / error',
    compressed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已压缩进摘要',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    INDEX idx_ai_chat_message_session (session_id, id),
    INDEX idx_ai_chat_message_student_time (student_id, created_at)
) COMMENT='学生端AI问答消息表';

INSERT INTO sys_config (k, v)
VALUES
    ('studentAiSummaryTriggerUncompressedCount', '14'),
    ('studentAiSummaryKeepRecentCount', '6'),
    ('studentAiSummaryMaxTokens', '800')
ON DUPLICATE KEY UPDATE v = VALUES(v);

INSERT INTO stu_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, route_path, icon, sort_num, description, status)
VALUES
    (1007, 0, 'menu:student:ai-chat', 'AI问答菜单', 'MENU', NULL, '/ai-chat', 'MessageOutlined', 7, '学生端AI问答入口', 1),
    (1110, 0, 'api:student:ai-chat:session:create', '创建AI问答会话', 'API', '/student-api/ai-chat/sessions', NULL, NULL, 20, '创建学生AI问答会话接口', 1),
    (1111, 0, 'api:student:ai-chat:session:list', '查询AI问答会话', 'API', '/student-api/ai-chat/sessions', NULL, NULL, 21, '查询学生AI问答会话列表接口', 1),
    (1112, 0, 'api:student:ai-chat:message:list', '查询AI问答消息', 'API', '/student-api/ai-chat/sessions/*/messages', NULL, NULL, 22, '查询学生AI问答消息接口', 1),
    (1113, 0, 'api:student:ai-chat:message:send', '发送AI问答消息', 'API', '/student-api/ai-chat/sessions/*/messages', NULL, NULL, 23, '发送学生AI问答消息接口', 1),
    (1114, 0, 'api:student:ai-chat:session:delete', '删除AI问答会话', 'API', '/student-api/ai-chat/sessions/*', NULL, NULL, 24, '删除学生AI问答会话接口', 1),
    (1115, 0, 'api:student:ai-chat:message:stream', '流式AI问答消息', 'API', '/student-api/ai-chat/sessions/*/messages/stream', NULL, NULL, 25, '流式发送学生AI问答消息接口', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    route_path = VALUES(route_path),
    icon = VALUES(icon),
    sort_num = VALUES(sort_num),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO stu_template_permission (template_id, permission_id)
VALUES
    (2001, 1007), (2001, 1110), (2001, 1111), (2001, 1112), (2001, 1113), (2001, 1114), (2001, 1115),
    (2002, 1007), (2002, 1110), (2002, 1111), (2002, 1112), (2002, 1113), (2002, 1114), (2002, 1115)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
