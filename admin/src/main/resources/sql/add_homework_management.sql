-- 作业管理表结构与权限初始化脚本

CREATE TABLE IF NOT EXISTS homework_assignment (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    title VARCHAR(200) NOT NULL COMMENT '作业标题',
    description TEXT NULL COMMENT '作业说明',
    class_id BIGINT UNSIGNED NOT NULL COMMENT '班级ID',
    homework_date DATE NULL COMMENT '作业日期',
    course_id BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
    course_detail_id BIGINT UNSIGNED NULL COMMENT '课程明细ID',
    course_content_snapshot VARCHAR(500) NULL COMMENT '发布时课程内容快照',
    attachment_name VARCHAR(255) NULL COMMENT '作业附件名称',
    attachment_url VARCHAR(500) NULL COMMENT '作业附件地址',
    attachment_type VARCHAR(50) NULL COMMENT '作业附件类型',
    answer_attachment_name VARCHAR(255) NULL COMMENT '答案附件名称',
    answer_attachment_url VARCHAR(500) NULL COMMENT '答案附件地址',
    answer_attachment_type VARCHAR(50) NULL COMMENT '答案附件类型',
    answer_publish_status VARCHAR(20) NOT NULL DEFAULT 'UNPUBLISHED' COMMENT '答案发布状态：UNPUBLISHED/PUBLISHED',
    answer_publish_time DATETIME NULL COMMENT '答案发布时间',
    answer_publish_teacher_id BIGINT UNSIGNED NULL COMMENT '答案发布老师ID',
    publish_teacher_id BIGINT UNSIGNED NOT NULL COMMENT '发布老师ID',
    publish_date DATETIME NOT NULL COMMENT '发布时间',
    deadline DATETIME NULL COMMENT '截止时间',
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED/CLOSED',
    allow_resubmit TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许重交',
    student_count_snapshot INT NOT NULL DEFAULT 0 COMMENT '发布时班级应交人数快照',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0否1是',
    close_time DATETIME NULL COMMENT '关闭时间',
    close_teacher_id BIGINT UNSIGNED NULL COMMENT '关闭老师ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_class_id (class_id),
    KEY idx_course_detail_id (course_detail_id),
    KEY idx_publish_teacher_id (publish_teacher_id),
    KEY idx_status (status),
    KEY idx_homework_date (homework_date)
) COMMENT='作业发布表';

CREATE TABLE IF NOT EXISTS homework_submission (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    assignment_id BIGINT UNSIGNED NOT NULL COMMENT '作业ID',
    student_no VARCHAR(64) NOT NULL COMMENT '学员编号',
    student_name_snapshot VARCHAR(100) NULL COMMENT '学员姓名快照',
    submit_content TEXT NULL COMMENT '提交内容',
    attachment_name VARCHAR(255) NULL COMMENT '提交附件名称',
    attachment_url VARCHAR(500) NULL COMMENT '提交附件地址',
    attachment_type VARCHAR(50) NULL COMMENT '提交附件类型',
    submit_time DATETIME NULL COMMENT '提交时间',
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED/REVIEWED/RESUBMIT_REQUIRED',
    score DECIMAL(5,2) NULL COMMENT '得分',
    teacher_comment TEXT NULL COMMENT '老师评语',
    review_teacher_id BIGINT UNSIGNED NULL COMMENT '批改老师ID',
    review_time DATETIME NULL COMMENT '批改时间',
    version INT NOT NULL DEFAULT 1 COMMENT '提交版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_assignment_student (assignment_id, student_no),
    KEY idx_student_no (student_no),
    KEY idx_status (status),
    KEY idx_review_teacher_id (review_teacher_id)
) COMMENT='作业提交表';

INSERT INTO sys_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (121, 106, 'menu:teaching:homework', '作业管理', 'MENU', null, 7, '教学管理下的作业管理菜单', 1),
    (249, 121, 'button:homework:create', '发布作业按钮', 'BUTTON', null, 1, '作业管理页面的发布作业按钮', 1),
    (250, 121, 'button:homework:update', '编辑作业按钮', 'BUTTON', null, 2, '作业管理页面的编辑作业按钮', 1),
    (251, 121, 'button:homework:delete', '删除作业按钮', 'BUTTON', null, 3, '作业管理页面的删除作业按钮', 1),
    (252, 121, 'button:homework:toggle-status', '开关作业按钮', 'BUTTON', null, 4, '作业管理页面的开放关闭按钮', 1),
    (253, 121, 'button:homework:submission-list', '提交列表按钮', 'BUTTON', null, 5, '作业管理页面的提交列表按钮', 1),
    (254, 121, 'button:homework:answer-manage', '答案管理按钮', 'BUTTON', null, 6, '作业管理页面的答案管理按钮', 1),
    (65, 121, 'homework:view', '查看作业', 'API', '/api/teaching/homeworks**', 42, '查看作业列表、详情与发布上下文接口', 1),
    (66, 121, 'homework:create', '创建作业', 'API', '/api/teaching/homeworks', 43, '发布作业接口', 1),
    (67, 121, 'homework:update', '编辑作业', 'API', '/api/teaching/homeworks/*', 44, '编辑作业与删除作业接口', 1),
    (68, 121, 'homework:status', '变更作业状态', 'API', '/api/teaching/homeworks/*/status', 45, '作业开放关闭接口', 1),
    (69, 121, 'homework:answer', '作业答案管理', 'API', '/api/teaching/homeworks/*/answer', 46, '作业答案管理接口', 1),
    (70, 121, 'homework:submission', '查看作业提交', 'API', '/api/teaching/homeworks/*/submissions**', 47, '查看作业提交列表接口', 1),
    (71, 121, 'homework:review', '批改作业提交', 'API', '/api/teaching/homeworks/*/submissions/*/review', 48, '批改作业提交接口', 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), permission_type = VALUES(permission_type), api_path = VALUES(api_path), description = VALUES(description), status = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 65), (1, 66), (1, 67), (1, 68), (1, 69), (1, 70), (1, 71),
    (1, 121), (1, 249), (1, 250), (1, 251), (1, 252), (1, 253), (1, 254)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
