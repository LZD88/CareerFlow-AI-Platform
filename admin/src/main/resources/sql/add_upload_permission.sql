-- 文件上传权限初始化脚本

INSERT INTO sys_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (56, 101, 'upload:file', '文件上传', 'API', '/api/upload/**', 33, '上传文件与生成直传地址接口', 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), permission_type = VALUES(permission_type), api_path = VALUES(api_path), description = VALUES(description), status = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 56)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
