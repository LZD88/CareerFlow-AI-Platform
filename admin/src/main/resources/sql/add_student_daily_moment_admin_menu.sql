-- 学员管理下的生活剪影菜单与接口权限

INSERT INTO sys_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (120, 117, 'menu:student:daily-moment', '生活剪影', 'MENU', NULL, 2, '学员管理下的生活剪影菜单', 1),
    (64, 120, 'student:daily-moment:view', '查看生活剪影', 'API', '/api/students/daily-moments**', 41, '查看学员生活剪影列表接口', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 64),
    (1, 120)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
