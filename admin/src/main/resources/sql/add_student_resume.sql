CREATE TABLE IF NOT EXISTS student_resume (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '简历ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    student_code VARCHAR(64) NOT NULL COMMENT '学生编号',
    student_name VARCHAR(100) NOT NULL COMMENT '学生姓名',
    resume_name VARCHAR(100) NOT NULL COMMENT '简历名称',
    target_role VARCHAR(100) DEFAULT NULL COMMENT '目标岗位',
    content_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '内容类型：TEXT/FILE',
    resume_text MEDIUMTEXT NOT NULL COMMENT '简历文本',
    resume_summary TEXT DEFAULT NULL COMMENT '简历摘要',
    resume_file_url VARCHAR(500) DEFAULT NULL COMMENT '简历文件地址',
    parse_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '解析状态：PENDING/SUCCESS/FAILED',
    is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认简历',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_student_resume_student (student_id, status, is_default),
    KEY idx_student_resume_code (student_code),
    KEY idx_student_resume_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生简历';

INSERT INTO sys_permission
    (id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (123, 117, 'menu:student:resume', '简历管理', 'MENU', NULL, 3, '学员管理下的简历管理菜单', 1),
    (255, 123, 'student:resume:view', '查看学生简历', 'API', '/api/student-resumes**', 42, '查看后台学生简历列表和详情', 1),
    (256, 123, 'student:resume:delete', '删除学生简历', 'API', '/api/student-resumes/*', 43, '后台删除学生简历', 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    sort_num = VALUES(sort_num),
    description = VALUES(description),
    status = VALUES(status);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id
FROM sys_permission
WHERE permission_code IN ('menu:student:resume', 'student:resume:view', 'student:resume:delete');

INSERT INTO stu_permission
(id, parent_id, permission_code, permission_name, permission_type, api_path, route_path, icon, sort_num, description, status)
VALUES
    (1009, 0, 'menu:student:resume', '我的简历菜单', 'MENU', NULL, '/resumes', 'ProfileOutlined', 9, '学生端我的简历入口', 1),
    (1117, 0, 'api:student:resume:list', '查看我的简历', 'API', '/student-api/resumes', NULL, NULL, 27, '查询当前登录学员简历列表接口', 1),
    (1118, 0, 'api:student:resume:create', '新增我的简历', 'API', '/student-api/resumes', NULL, NULL, 28, '新增当前登录学员简历接口', 1),
    (1119, 0, 'api:student:resume:update', '编辑我的简历', 'API', '/student-api/resumes/*', NULL, NULL, 29, '编辑当前登录学员简历接口', 1),
    (1120, 0, 'api:student:resume:default', '设置默认简历', 'API', '/student-api/resumes/*/default', NULL, NULL, 30, '设置当前登录学员默认简历接口', 1),
    (1121, 0, 'api:student:resume:delete', '删除我的简历', 'API', '/student-api/resumes/*', NULL, NULL, 31, '删除当前登录学员简历接口', 1),
    (1122, 0, 'api:student:resume:upload', '上传解析简历', 'API', '/student-api/resumes/upload', NULL, NULL, 32, '上传并解析当前登录学员简历接口', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    route_path = VALUES(route_path),
    icon = VALUES(icon),
    sort_num = VALUES(sort_num),
    description = VALUES(description),
    status = VALUES(status);

INSERT IGNORE INTO stu_template_permission (template_id, permission_id)
SELECT template_id, permission_id
FROM (
    SELECT 2001 AS template_id, id AS permission_id FROM stu_permission
    WHERE permission_code IN (
        'menu:student:resume',
        'api:student:resume:list',
        'api:student:resume:create',
                                   'api:student:resume:update',
                                   'api:student:resume:default',
                                   'api:student:resume:delete',
                                   'api:student:resume:upload'
             )
         UNION ALL
    SELECT 2002 AS template_id, id AS permission_id FROM stu_permission
    WHERE permission_code IN (
        'menu:student:resume',
        'api:student:resume:list',
        'api:student:resume:create',
                                   'api:student:resume:update',
                                   'api:student:resume:default',
                                   'api:student:resume:delete',
                                   'api:student:resume:upload'
             )
) p;
