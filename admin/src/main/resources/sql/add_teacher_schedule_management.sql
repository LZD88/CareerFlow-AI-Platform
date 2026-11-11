-- 老师课表菜单与接口权限初始化脚本
-- 适用于已执行过 init_permission_data.sql 的存量环境

INSERT INTO sys_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (111, 106, 'menu:teaching:teacher-schedule', '老师课表', 'MENU', null, 5, '教学管理下的老师课表菜单', 1),
    (234, 111, 'button:teacher-schedule:view', '老师课表查看按钮', 'BUTTON', null, 1, '老师课表页面的查看按钮', 1),
    (42, 111, 'teacher-schedule:view', '查看老师课表', 'API', '/api/teaching/schedules/teachers/calendar', 19, '查看老师维度课表接口', 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), permission_type = VALUES(permission_type), api_path = VALUES(api_path), description = VALUES(description), status = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 42), (1, 111), (1, 234)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
