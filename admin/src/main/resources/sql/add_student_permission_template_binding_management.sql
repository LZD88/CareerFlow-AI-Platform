-- 学生权限模板绑定管理权限初始化脚本

INSERT INTO sys_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (247, 118, 'button:student:bind-permission-template', '学生绑定权限模板按钮', 'BUTTON', NULL, 1, '学生管理页面的绑定权限模板按钮', 1),
    (248, 118, 'button:student:unbind-permission-template', '学生解绑权限模板按钮', 'BUTTON', NULL, 2, '学生管理页面的解绑权限模板按钮', 1),
    (62, 118, 'student:permission-template:view', '查看学生权限模板绑定', 'API', '/api/students/*/permission-template-detail', 39, '查看学生当前权限模板来源与绑定详情接口', 1),
    (63, 118, 'student:permission-template:update', '绑定学生权限模板', 'API', '/api/students/*/permission-template', 40, '给学生绑定权限模板接口', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    description = VALUES(description),
    status = VALUES(status);

UPDATE sys_permission
SET api_path = '/api/students'
WHERE id = 57;

INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 62),
    (1, 63),
    (1, 247),
    (1, 248)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
