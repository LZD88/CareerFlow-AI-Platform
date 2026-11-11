-- 日志分析权限初始化脚本

INSERT INTO sys_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (116, 101, 'menu:system:log-analysis', '日志分析', 'MENU', null, 5, '系统配置下的日志分析菜单', 1),
    (54, 116, 'log-analysis:search', '日志分析查询', 'API', '/api/log-analysis/search', 31, '查询火山日志并返回结构化结果接口', 1),
    (55, 116, 'log-analysis:diagnose', '日志诊断分析', 'API', '/api/log-analysis/diagnose', 32, '按 guid 查询日志并返回结构化诊断结果接口', 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), permission_type = VALUES(permission_type), api_path = VALUES(api_path), description = VALUES(description), status = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 54), (1, 55), (1, 116)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
