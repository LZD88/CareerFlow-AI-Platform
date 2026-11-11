-- 面试复盘权限初始化脚本

INSERT INTO sys_permission (parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (101, 'menu:system:interview-review', '面试复盘', 'MENU', NULL, 10, '系统配置下的面试复盘菜单', 1)
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
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'button:interview-review:upload', '面试复盘上传音频按钮', 'BUTTON', NULL, 1, '面试复盘页面的上传音频按钮', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'button:interview-review:transcribe', '面试复盘开始转写按钮', 'BUTTON', NULL, 2, '面试复盘页面的开始转写按钮', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'button:interview-review:review', '面试复盘生成报告按钮', 'BUTTON', NULL, 3, '面试复盘页面的生成报告按钮', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'button:interview-review:question-generate', '面试题库生成按钮', 'BUTTON', NULL, 4, '从面试转写生成问题总结按钮', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'interview-question:list', '面试问题题库列表接口', 'API', '/api/interview-questions', 90, '查询面试问题题库', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'interview-question:detail', '面试问题题库详情接口', 'API', '/api/interview-questions/*', 91, '查询面试问题详情', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'interview-question:sources', '面试问题来源接口', 'API', '/api/interview-questions/*/sources', 92, '查询面试问题来源面试记录', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'interview-question:generate', '面试问题题库生成接口', 'API', '/api/interview-questions/generate-from-task/*', 93, '从面试复盘任务提取问题并沉淀题库', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'interview-review:list', '面试复盘列表接口', 'API', '/api/interview-reviews', 100, '查询面试复盘任务列表', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'interview-review:detail', '面试复盘详情接口', 'API', '/api/interview-reviews/*', 101, '查询面试复盘任务详情', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'interview-review:upload-audio', '面试复盘上传音频接口', 'API', '/api/interview-reviews/audio', 102, '上传面试音频并创建复盘任务', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'interview-review:audio-presign', '面试复盘音频直传签名接口', 'API', '/api/interview-reviews/audio/presign', 103, '生成面试音频 TOS 直传地址', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'interview-review:audio-confirm', '面试复盘音频上传确认接口', 'API', '/api/interview-reviews/audio/confirm', 104, '确认面试音频已直传并创建任务', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'interview-review:resume-confirm', '面试复盘简历上传确认接口', 'API', '/api/interview-reviews/*/resume', 105, '给已有面试复盘任务上传或替换简历', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'interview-review:transcribe', '面试复盘音频转写接口', 'API', '/api/interview-reviews/*/transcribe', 106, '调用 Python Agent 转写面试音频', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:interview-review') interview_review_menu),
     'interview-review:review', '面试复盘报告生成接口', 'API', '/api/interview-reviews/*/review', 107, '调用 Python Agent 生成面试复盘报告', 1)
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
    'menu:system:interview-review',
    'button:interview-review:upload',
    'button:interview-review:transcribe',
    'button:interview-review:review',
    'button:interview-review:question-generate',
    'interview-question:list',
    'interview-question:detail',
    'interview-question:sources',
    'interview-question:generate',
    'interview-review:list',
    'interview-review:detail',
    'interview-review:upload-audio',
    'interview-review:audio-presign',
    'interview-review:audio-confirm',
    'interview-review:resume-confirm',
    'interview-review:transcribe',
    'interview-review:review'
)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
