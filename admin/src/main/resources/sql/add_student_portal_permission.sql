-- 学生端权限基础表
-- 适用于已执行过 init.sql 的存量环境

-- 学生端权限表：统一存储菜单、按钮、接口权限点
CREATE TABLE IF NOT EXISTS stu_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父权限ID，根节点为0',
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    permission_type VARCHAR(32) NOT NULL COMMENT '权限类型，例如API、MENU、BUTTON',
    api_path VARCHAR(255) NULL COMMENT 'API路径匹配规则，仅API权限使用',
    route_path VARCHAR(255) NULL COMMENT '前端路由路径，菜单权限可用',
    icon VARCHAR(64) NULL COMMENT '菜单图标',
    sort_num INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    description VARCHAR(255) NULL COMMENT '权限描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态，1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='学生端权限表';

-- 学生端权限模板表：按学习方式、产品、班级等维度组织权限模板
CREATE TABLE IF NOT EXISTS stu_permission_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模板ID',
    template_code VARCHAR(64) NOT NULL UNIQUE COMMENT '模板编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    study_mode VARCHAR(32) NULL COMMENT '学习方式：ONLINE、OFFLINE，为空表示通用',
    product_id BIGINT NULL COMMENT '产品ID，为空表示通用',
    class_id BIGINT NULL COMMENT '班级ID，为空表示通用',
    description VARCHAR(255) NULL COMMENT '模板描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态，1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_study_mode (study_mode),
    KEY idx_product_id (product_id),
    KEY idx_class_id (class_id)
) COMMENT='学生端权限模板表';

-- 模板权限关联表：一个模板可以关联多个权限点
CREATE TABLE IF NOT EXISTS stu_template_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_template_permission (template_id, permission_id),
    KEY idx_permission_id (permission_id)
) COMMENT='学生端模板权限关联表';

-- 学生模板绑定表：用于给学员分配权限模板
CREATE TABLE IF NOT EXISTS stu_student_permission_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    student_id BIGINT NOT NULL COMMENT '学员ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    effective_start_time DATETIME NULL COMMENT '生效开始时间',
    effective_end_time DATETIME NULL COMMENT '生效结束时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态，1生效，0失效',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_student_template (student_id, template_id),
    KEY idx_template_id (template_id),
    KEY idx_effective_time (effective_start_time, effective_end_time)
) COMMENT='学生端学员模板绑定表';

-- 初始化学生端权限点
INSERT INTO stu_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, route_path, icon, sort_num, description, status)
VALUES
    (1001, 0, 'menu:student:profile', '学员信息菜单', 'MENU', NULL, '/learning-home?tab=profile', 'UserOutlined', 1, '学生端首页的学员信息菜单', 1),
    (1002, 0, 'menu:student:course', '课程信息菜单', 'MENU', NULL, '/learning-home?tab=courses', 'ReadOutlined', 2, '学生端首页的课程信息菜单', 1),
    (1003, 0, 'menu:student:schedule', '课表安排菜单', 'MENU', NULL, '/learning-home?tab=schedule', 'CalendarOutlined', 3, '学生端首页的课表安排菜单', 1),
    (1004, 0, 'menu:student:notice', '通知消息菜单', 'MENU', NULL, '/learning-home?tab=notice', 'NotificationOutlined', 4, '学生端首页的通知消息菜单', 1),
    (1005, 0, 'menu:student:daily-moment', '我的剪影菜单', 'MENU', NULL, '/learning-home?tab=moments', 'CameraOutlined', 5, '学生端首页的我的剪影入口', 1),
    (1006, 0, 'menu:student:homework', '我的作业菜单', 'MENU', NULL, '/learning-home?tab=homework', 'ReadOutlined', 6, '学生端首页的我的作业入口', 1),
    (1102, 0, 'api:student:profile:view', '查看学员资料', 'API', '/student-api/profile', NULL, NULL, 12, '查询当前登录学员资料接口', 1),
    (1103, 0, 'api:student:daily-moment:list', '查看日常剪影', 'API', '/student-api/daily-moments', NULL, NULL, 13, '查询当前登录学员日常剪影列表接口', 1),
    (1104, 0, 'api:student:daily-moment:create', '新增日常剪影', 'API', '/student-api/daily-moments', NULL, NULL, 14, '新增当前登录学员日常剪影接口', 1),
    (1105, 0, 'api:student:daily-moment:upload', '日常剪影上传素材', 'API', '/student-api/daily-moments/upload-presign', NULL, NULL, 15, '生成日常剪影素材直传地址接口', 1),
    (1106, 0, 'api:student:homework:list', '查看我的作业', 'API', '/student-api/homeworks', NULL, NULL, 16, '查询当前登录学员作业列表接口', 1),
    (1107, 0, 'api:student:homework:detail', '查看作业详情', 'API', '/student-api/homeworks/*', NULL, NULL, 17, '查询当前登录学员作业详情接口', 1),
    (1108, 0, 'api:student:homework:submit', '提交作业', 'API', '/student-api/homeworks/*/submit', NULL, NULL, 18, '提交当前登录学员作业接口', 1),
    (1109, 0, 'api:student:homework:upload', '作业附件上传', 'API', '/student-api/homeworks/upload-presign', NULL, NULL, 19, '生成学生作业附件直传地址接口', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    route_path = VALUES(route_path),
    icon = VALUES(icon),
    sort_num = VALUES(sort_num),
    description = VALUES(description),
    status = VALUES(status);

-- 初始化学生端权限模板
INSERT INTO stu_permission_template (id, template_code, template_name, study_mode, product_id, class_id, description, status)
VALUES
    (2001, 'FULL_ONLINE_COMMON', '线上学员通用模板', 'ONLINE', NULL, NULL, '适用于正式开通后的线上学员基础权限', 1),
    (2002, 'FULL_OFFLINE_COMMON', '线下学员通用模板', 'OFFLINE', NULL, NULL, '适用于正式开通后的线下学员基础权限', 1)
ON DUPLICATE KEY UPDATE
    template_name = VALUES(template_name),
    study_mode = VALUES(study_mode),
    product_id = VALUES(product_id),
    class_id = VALUES(class_id),
    description = VALUES(description),
    status = VALUES(status);

-- 初始化模板与权限点关系
INSERT INTO stu_template_permission (template_id, permission_id)
VALUES
    (2001, 1001),
    (2001, 1002),
    (2001, 1003),
    (2001, 1004),
    (2001, 1005),
    (2001, 1006),
    (2001, 1101),
    (2001, 1102),
    (2001, 1103),
    (2001, 1104),
    (2001, 1105),
    (2001, 1106),
    (2001, 1107),
    (2001, 1108),
    (2001, 1109),
    (2002, 1001),
    (2002, 1003),
    (2002, 1004),
    (2002, 1005),
    (2002, 1006),
    (2002, 1101),
    (2002, 1102),
    (2002, 1103),
    (2002, 1104),
    (2002, 1105),
    (2002, 1106),
    (2002, 1107),
    (2002, 1108),
    (2002, 1109)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
