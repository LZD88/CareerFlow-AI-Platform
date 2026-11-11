-- 学员权限模板管理后台权限初始化脚本
-- 适用于已执行过 init.sql 的存量环境

INSERT INTO sys_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (119, 101, 'menu:system:student-permission-template', '学员权限模板', 'MENU', NULL, 4, '系统配置下的学员权限模板菜单', 1),
    (243, 119, 'button:student-permission-template:create', '学员权限模板新增按钮', 'BUTTON', NULL, 1, '学员权限模板页面的新增按钮', 1),
    (244, 119, 'button:student-permission-template:update', '学员权限模板编辑按钮', 'BUTTON', NULL, 2, '学员权限模板页面的编辑按钮', 1),
    (245, 119, 'button:student-permission-template:delete', '学员权限模板删除按钮', 'BUTTON', NULL, 3, '学员权限模板页面的删除按钮', 1),
    (246, 119, 'button:student-permission-template:assign-permission', '学员权限模板分配权限按钮', 'BUTTON', NULL, 4, '学员权限模板页面的分配权限按钮', 1),
    (58, 119, 'student-permission-template:view', '查看学员权限模板', 'API', '/api/system/student-permission-templates/**', 35, '查看学员权限模板列表与详情接口', 1),
    (59, 119, 'student-permission-template:create', '创建学员权限模板', 'API', '/api/system/student-permission-templates', 36, '新增学员权限模板接口', 1),
    (60, 119, 'student-permission-template:update', '编辑学员权限模板', 'API', '/api/system/student-permission-templates/*', 37, '编辑学员权限模板接口', 1),
    (61, 119, 'student-permission-template:assign-permission', '学员权限模板分配权限', 'API', '/api/system/student-permission-templates/*/permissions', 38, '给学员权限模板绑定学生权限点接口', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    sort_num = VALUES(sort_num),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 58), (1, 59), (1, 60), (1, 61),
    (1, 119), (1, 243), (1, 244), (1, 245), (1, 246)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
