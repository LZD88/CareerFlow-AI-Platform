CREATE TABLE IF NOT EXISTS mock_interview_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    student_id BIGINT NOT NULL COMMENT '学生 ID',
    student_code VARCHAR(64) NOT NULL COMMENT '学号',
    student_name VARCHAR(100) DEFAULT NULL COMMENT '学生姓名',
    title VARCHAR(100) NOT NULL COMMENT '模拟面试标题',
    interview_role VARCHAR(100) DEFAULT NULL COMMENT '面试岗位',
    status VARCHAR(32) NOT NULL COMMENT '状态：CREATED/IN_PROGRESS/FINISHED/FAILED',
    volc_session_id VARCHAR(64) NOT NULL COMMENT '火山 Realtime 会话 ID',
    realtime_token VARCHAR(1024) DEFAULT NULL COMMENT '实时网关短期 token',
    realtime_token_expires_at DATETIME DEFAULT NULL COMMENT '实时网关 token 过期时间',
    transcript_text LONGTEXT DEFAULT NULL COMMENT '面试转写文本',
    fail_reason VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
    started_at DATETIME DEFAULT NULL COMMENT '开始时间',
    ended_at DATETIME DEFAULT NULL COMMENT '结束时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_mock_interview_student (student_id, id),
    KEY idx_mock_interview_status (status),
    UNIQUE KEY uk_mock_interview_volc_session (volc_session_id)
) COMMENT='学生 AI 模拟面试会话';

INSERT INTO stu_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, route_path, icon, sort_num, description, status)
VALUES
    (1008, 0, 'menu:student:mock-interview', 'AI模拟面试菜单', 'MENU', NULL, '/mock-interview', 'AudioOutlined', 8, '学生端AI模拟面试入口', 1),
    (1116, 0, 'api:student:mock-interview:session:create', '创建AI模拟面试会话', 'API', '/student-api/mock-interviews/sessions', NULL, NULL, 26, '创建学生AI模拟面试会话接口', 1)
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
    (2001, 1008), (2001, 1116),
    (2002, 1008), (2002, 1116)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
