-- 学生日常剪影权限脚本
-- 适用于已执行过 add_student_portal_permission.sql 的存量环境

INSERT INTO stu_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, route_path, icon, sort_num, description, status)
VALUES
    (1005, 0, 'menu:student:daily-moment', '我的剪影菜单', 'MENU', NULL, '/learning-home?tab=moments', 'CameraOutlined', 5, '学生端首页的我的剪影入口', 1),
    (1103, 0, 'api:student:daily-moment:list', '查看日常剪影', 'API', '/student-api/daily-moments', NULL, NULL, 13, '查询当前登录学员日常剪影列表接口', 1),
    (1104, 0, 'api:student:daily-moment:create', '新增日常剪影', 'API', '/student-api/daily-moments', NULL, NULL, 14, '新增当前登录学员日常剪影接口', 1),
    (1105, 0, 'api:student:daily-moment:upload', '日常剪影上传素材', 'API', '/student-api/daily-moments/upload-presign', NULL, NULL, 15, '生成日常剪影素材直传地址接口', 1)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    route_path = VALUES(route_path),
    icon = VALUES(icon),
    sort_num = VALUES(sort_num),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO stu_template_permission (template_id, permission_id)
VALUES
    (2001, 1005),
    (2002, 1005),
    (2001, 1103),
    (2002, 1103),
    (2001, 1104),
    (2001, 1105),
    (2002, 1104),
    (2002, 1105)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
