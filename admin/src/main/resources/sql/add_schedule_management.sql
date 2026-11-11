-- 班级课表表与权限初始化脚本
-- 适用于已执行过 init.sql 的存量环境

CREATE TABLE IF NOT EXISTS edu_class_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课表ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    course_detail_id BIGINT NULL COMMENT '课程详情ID，正式上课日才有值',
    course_day_num INT NULL COMMENT '课程第几天，正式上课日才有值',
    schedule_date DATE NOT NULL COMMENT '排课日期',
    schedule_type VARCHAR(32) NOT NULL DEFAULT 'CLASS' COMMENT '排课类型 CLASS/SELF_STUDY/REST',
    course_content VARCHAR(2000) NULL COMMENT '课程内容或当天安排说明',
    stage_name VARCHAR(100) NULL COMMENT '课程所属阶段，自习/休息可为空',
    teacher_id BIGINT NULL COMMENT '当天授课老师ID，仅上课日可能有值',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_class_schedule_date (class_id, schedule_date),
    UNIQUE KEY uk_class_course_day (class_id, course_day_num),
    KEY idx_course_id (course_id),
    KEY idx_course_detail_id (course_detail_id),
    KEY idx_teacher_id (teacher_id)
) COMMENT='班级课表表';

INSERT INTO sys_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (110, 106, 'menu:teaching:schedule', '课表管理', 'MENU', null, 4, '教学管理下的课表管理菜单', 1),
    (230, 109, 'button:class:generate-schedule', '生成课表按钮', 'BUTTON', null, 4, '班级管理页面的生成课表按钮', 1),
    (231, 110, 'button:schedule:view', '课表查看按钮', 'BUTTON', null, 1, '课表管理页面的查看按钮', 1),
    (232, 110, 'button:schedule:arrange', '课表调整按钮', 'BUTTON', null, 2, '课表管理页面的新增课程按钮', 1),
    (233, 110, 'button:schedule:assign-teacher', '分配老师按钮', 'BUTTON', null, 3, '课表管理页面的分配老师按钮', 1),
    (38, 110, 'schedule:view', '查看课表', 'API', '/api/teaching/schedules/classes/*/calendar', 11, '查看班级月度课表接口', 1),
    (39, 109, 'schedule:generate', '生成课表', 'API', '/api/teaching/schedules/classes/*/generate', 12, '按课程详情生成班级课表接口', 1),
    (40, 110, 'schedule:arrange', '调整课表', 'API', '/api/teaching/schedules/classes/*/arrange', 13, '调整班级单日课表接口', 1),
    (41, 110, 'schedule:assign-teacher', '分配授课老师', 'API', '/api/teaching/schedules/classes/*/stage-teachers', 14, '按阶段批量分配每日授课老师接口', 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), permission_type = VALUES(permission_type), api_path = VALUES(api_path), description = VALUES(description), status = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 38), (1, 39), (1, 40), (1, 41), (1, 106), (1, 109), (1, 110), (1, 230), (1, 231), (1, 232), (1, 233)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
