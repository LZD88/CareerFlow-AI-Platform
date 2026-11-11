-- 数据分析权限初始化脚本
--
-- 页面入口放在“系统配置”下：
-- 1. 菜单权限控制前端是否显示“数据分析”页面
-- 2. 按钮权限控制页面上的“开始分析”按钮
-- 3. API 权限控制普通管理员是否能访问 Java 数据分析接口

INSERT INTO sys_permission (parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (101, 'menu:system:data-analysis', '数据分析', 'MENU', NULL, 8, '系统配置下的数据分析菜单', 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    sort_num = VALUES(sort_num),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO sys_permission (parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:data-analysis') data_analysis_menu),
     'button:data-analysis:analyze', '数据分析执行按钮', 'BUTTON', NULL, 1, '数据分析页面的开始分析按钮', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:data-analysis') data_analysis_menu),
     'data-analysis:plan', '数据分析选表接口', 'API', '/api/data-analysis/plan', 80, '根据自然语言问题选择候选业务表', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:data-analysis') data_analysis_menu),
     'data-analysis:analyze', '数据分析查询接口', 'API', '/api/data-analysis/analyze', 81, '生成 SQL、查询数据库并返回 AI 解读结果', 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    sort_num = VALUES(sort_num),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id
FROM sys_permission
WHERE permission_code IN (
    'menu:system:data-analysis',
    'button:data-analysis:analyze',
    'data-analysis:plan',
    'data-analysis:analyze'
)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
