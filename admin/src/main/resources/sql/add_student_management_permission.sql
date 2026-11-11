-- 学员管理权限初始化脚本
-- 适用于已执行过 init.sql 的存量环境

INSERT INTO sys_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (117, 0, 'menu:student', '学员管理', 'MENU', NULL, 4, '左侧导航中的学员管理父级菜单', 1),
    (118, 117, 'menu:student:manage', '学生管理', 'MENU', NULL, 1, '学员管理下的学生管理菜单', 1),
    (57, 118, 'student:view', '查看学生', 'API', '/api/students/**', 34, '查看学生列表接口', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    sort_num = VALUES(sort_num),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 57),
    (1, 117),
    (1, 118)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
