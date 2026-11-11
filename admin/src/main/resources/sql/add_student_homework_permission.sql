-- 学生端我的作业权限增量脚本

INSERT INTO stu_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, route_path, icon, sort_num, description, status)
VALUES
    (1006, 0, 'menu:student:homework', '我的作业菜单', 'MENU', NULL, '/learning-home?tab=homework', 'ReadOutlined', 6, '学生端首页的我的作业入口', 1),
    (1106, 0, 'api:student:homework:list', '查看我的作业', 'API', '/student-api/homeworks', NULL, NULL, 16, '查询当前登录学员作业列表接口', 1),
    (1107, 0, 'api:student:homework:detail', '查看作业详情', 'API', '/student-api/homeworks/*', NULL, NULL, 17, '查询当前登录学员作业详情接口', 1),
    (1108, 0, 'api:student:homework:submit', '提交作业', 'API', '/student-api/homeworks/*/submit', NULL, NULL, 18, '提交当前登录学员作业接口', 1),
    (1109, 0, 'api:student:homework:upload', '作业附件上传', 'API', '/student-api/homeworks/upload-presign', NULL, NULL, 19, '生成学生作业附件直传地址接口', 1)
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
    (2001, 1006), (2001, 1106), (2001, 1107), (2001, 1108), (2001, 1109),
    (2002, 1006), (2002, 1106), (2002, 1107), (2002, 1108), (2002, 1109)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
