-- 系统参数页面权限增量脚本
-- 用途：
-- 1. 新增“系统参数”菜单
-- 2. 新增系统参数页面按钮权限
-- 3. 新增系统参数接口权限
-- 4. 给超级管理员角色授权

-- 系统参数菜单权限
INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status
)
VALUES
    (105, 101, 'menu:system:config', '系统参数', 'MENU', NULL, 4, '系统配置下的系统参数菜单', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    description = VALUES(description),
    status = VALUES(status);

-- 系统参数按钮权限
INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status
)
VALUES
    (212, 105, 'button:config:create', '系统参数新增按钮', 'BUTTON', NULL, 1, '系统参数页面的新增配置按钮', 1),
    (213, 105, 'button:config:update', '系统参数编辑按钮', 'BUTTON', NULL, 2, '系统参数页面的编辑配置按钮', 1),
    (214, 105, 'button:config:delete', '系统参数删除按钮', 'BUTTON', NULL, 3, '系统参数页面的删除配置按钮', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    description = VALUES(description),
    status = VALUES(status);

-- 系统参数 API 权限
INSERT INTO sys_permission (
    id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status
)
VALUES
    (16, 105, 'config:view', '查看系统参数', 'API', '/api/system/configs/**', 11, '查看系统参数列表与详情接口', 1),
    (17, 105, 'config:create', '创建系统参数', 'API', '/api/system/configs', 12, '新增系统参数接口', 1),
    (18, 105, 'config:update', '编辑系统参数', 'API', '/api/system/configs/*', 13, '编辑系统参数接口', 1),
    (19, 105, 'config:delete', '删除系统参数', 'API', '/api/system/configs/*', 14, '删除系统参数接口', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    description = VALUES(description),
    status = VALUES(status);

-- 给超级管理员角色授权
INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 105),
    (1, 212),
    (1, 213),
    (1, 214),
    (1, 16),
    (1, 17),
    (1, 18),
    (1, 19)
ON DUPLICATE KEY UPDATE
    permission_id = VALUES(permission_id);
