-- 自媒体 Agent 权限初始化脚本
--
-- 第一版建设“选题生成 + 文字草稿生成 + 草稿保存”能力：
-- 1. 菜单权限控制前端是否显示“自媒体创作”页面
-- 2. 按钮权限控制页面上的“生成选题”“生成文字草稿”和“保存草稿”按钮
-- 3. API 权限控制普通管理员是否能访问 Java 自媒体 Agent 接口

INSERT INTO sys_permission (parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (101, 'menu:system:media-agent', '自媒体创作', 'MENU', NULL, 9, '系统配置下的自媒体创作菜单', 1)
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
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:media-agent') media_agent_menu),
     'button:media-agent:generate-topics', '自媒体生成选题按钮', 'BUTTON', NULL, 1, '自媒体创作页面的生成选题按钮', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:media-agent') media_agent_menu),
     'button:media-agent:generate-text-draft', '自媒体生成文字草稿按钮', 'BUTTON', NULL, 2, '自媒体创作页面的生成文字草稿按钮', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:media-agent') media_agent_menu),
     'button:media-agent:save-draft', '自媒体保存草稿按钮', 'BUTTON', NULL, 3, '自媒体创作页面的保存草稿按钮', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:media-agent') media_agent_menu),
     'media-agent:topics', '自媒体选题生成接口', 'API', '/api/media-agent/topics', 90, '根据创作主题生成自媒体选题方向', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:media-agent') media_agent_menu),
     'media-agent:text-draft', '自媒体文字草稿生成接口', 'API', '/api/media-agent/drafts/generate-text', 91, '根据选题方向生成平台文字草稿', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:media-agent') media_agent_menu),
     'media-agent:save-draft', '自媒体草稿保存接口', 'API', '/api/media-agent/drafts/save', 92, '保存编辑后的自媒体文字草稿', 1)
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
    'menu:system:media-agent',
    'button:media-agent:generate-topics',
    'button:media-agent:generate-text-draft',
    'button:media-agent:save-draft',
    'media-agent:topics',
    'media-agent:text-draft',
    'media-agent:save-draft'
)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
