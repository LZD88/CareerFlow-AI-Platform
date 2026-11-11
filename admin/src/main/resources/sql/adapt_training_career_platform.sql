-- 职业培训与就业服务一体化平台：权限文案及办理入班权限。
-- 原有业务建表与权限初始化脚本之后执行；可重复执行，不修改历史订单和学员数据。
UPDATE sys_permission SET permission_name = CASE permission_code
    WHEN 'menu:sales' THEN '招生报名'
    WHEN 'menu:sales:product' THEN '课程商品'
    WHEN 'menu:sales:order' THEN '报名订单'
    WHEN 'menu:sales:payment-order' THEN '支付流水'
    WHEN 'menu:teaching' THEN '教学履约'
    WHEN 'menu:teaching:campus' THEN '培训校区'
    WHEN 'menu:teaching:course' THEN '课程体系'
    WHEN 'menu:teaching:class' THEN '培训班级'
    WHEN 'menu:teaching:schedule' THEN '教学排课'
    WHEN 'menu:teaching:teacher-schedule' THEN '师资安排'
    WHEN 'menu:teaching:duty' THEN '值班安排'
    WHEN 'menu:teaching:homework' THEN '作业考核'
    WHEN 'menu:student' THEN '学员档案'
    WHEN 'menu:student:manage' THEN '学员档案'
    WHEN 'menu:student:daily-moment' THEN '学员动态'
    WHEN 'menu:student:resume' THEN '求职简历'
    WHEN 'menu:system' THEN '系统管理'
    WHEN 'menu:system:user' THEN '员工账号'
    WHEN 'menu:system:role' THEN '岗位角色'
    WHEN 'menu:system:permission' THEN '菜单与接口权限'
    WHEN 'menu:system:student-permission-template' THEN '学员权益模板'
    WHEN 'menu:system:knowledge-document' THEN '培训知识库'
    WHEN 'menu:system:data-analysis' THEN '业务数据分析'
    WHEN 'menu:system:interview-review' THEN 'AI 面试复盘'
    WHEN 'menu:system:media-agent' THEN '招生内容创作'
    WHEN 'menu:system:log-analysis' THEN '运行日志诊断'
    ELSE permission_name END;

INSERT INTO sys_permission (parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
SELECT id, 'button:order:enroll', '办理入班', 'BUTTON', NULL, 4, '为已支付报名订单建档入班', 1
FROM sys_permission WHERE permission_code = 'menu:sales:order'
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), description = VALUES(description), status = 1;

INSERT INTO sys_permission (parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
SELECT id, 'order:enroll', '报名入班接口', 'API', '/api/sales/orders/*/enrollment', 20, '已支付订单关联学员档案与班级', 1
FROM sys_permission WHERE permission_code = 'menu:sales:order'
ON DUPLICATE KEY UPDATE api_path = VALUES(api_path), description = VALUES(description), status = 1;

-- 订单编辑岗位补充入班权限；查询岗位不自动增加写权限。
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT DISTINCT rp.role_id, target.id
FROM sys_role_permission rp
JOIN sys_permission source ON source.id = rp.permission_id
CROSS JOIN sys_permission target
WHERE source.permission_code IN ('button:order:update', 'order:update')
  AND target.permission_code IN ('button:order:enroll', 'order:enroll');
