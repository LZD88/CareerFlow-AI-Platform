-- 用户表：存储后台用户基础信息
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '登录用户名',
    password VARCHAR(255) NOT NULL COMMENT '登录密码，当前为测试阶段明文存储',
    nickname VARCHAR(64) NOT NULL COMMENT '用户昵称',
    real_name VARCHAR(64) NULL COMMENT '真实姓名',
    phone VARCHAR(20) NULL COMMENT '手机号',
    email VARCHAR(128) NULL COMMENT '邮箱',
    union_id VARCHAR(128) NULL UNIQUE COMMENT '飞书 union_id',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态，1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='用户表';

-- 角色表：存储系统角色定义
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(64) NOT NULL UNIQUE COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) NULL COMMENT '角色描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态，1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='角色表';

-- 权限表：存储接口、菜单、按钮等权限点
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父权限ID，根节点为0',
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    permission_type VARCHAR(32) NOT NULL COMMENT '权限类型，例如API、MENU、BUTTON',
    api_path VARCHAR(255) NULL COMMENT 'API路径匹配规则，仅API权限使用',
    sort_num INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    description VARCHAR(255) NULL COMMENT '权限描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态，1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='权限表';

-- 用户角色关联表：用户通过角色继承权限
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id)
) COMMENT='用户角色关联表';

-- 角色权限关联表：角色与权限的多对多关系
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) COMMENT='角色权限关联表';

-- 系统配置表：存储可动态调整的运行时配置
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    k VARCHAR(100) NOT NULL UNIQUE COMMENT '配置Key',
    v VARCHAR(500) NOT NULL COMMENT '配置Value'
) COMMENT='系统配置表';

-- 校区表：存储教学管理中的校区基础信息
CREATE TABLE IF NOT EXISTS edu_campus (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '校区ID',
    campus_name VARCHAR(100) NOT NULL COMMENT '校区名称',
    principal_user_id BIGINT NOT NULL COMMENT '负责人用户ID',
    address VARCHAR(255) NOT NULL COMMENT '校区地址',
    contact_phone VARCHAR(20) NULL COMMENT '联系电话',
    remark VARCHAR(500) NULL COMMENT '备注',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态，1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_principal_user_id (principal_user_id)
) COMMENT='校区表';

-- 课程表：存储教学管理中的课程基础信息
CREATE TABLE IF NOT EXISTS edu_course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
    course_name VARCHAR(100) NOT NULL COMMENT '课程名称',
    course_days INT NOT NULL COMMENT '课程天数',
    feishu_material_path VARCHAR(500) NOT NULL COMMENT '飞书资料路径',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态，1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='课程表';

-- 课程详情表：存储课程中每天的具体内容与所属阶段
CREATE TABLE IF NOT EXISTS edu_course_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程详情ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    stage_name VARCHAR(100) NOT NULL COMMENT '所属阶段',
    day_num INT NOT NULL COMMENT '第几天课',
    course_content VARCHAR(2000) NOT NULL COMMENT '课程内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_course_id (course_id),
    KEY idx_stage_day (stage_name, day_num)
) COMMENT='课程详情表';

-- 班级表：存储班级期数、班主任、状态、开班时间和课程等信息
CREATE TABLE IF NOT EXISTS edu_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级ID',
    class_term INT NOT NULL COMMENT '班级期数',
    campus_id BIGINT NOT NULL COMMENT '校区ID',
    head_teacher_id BIGINT NOT NULL COMMENT '班主任用户ID',
    class_status VARCHAR(32) NOT NULL COMMENT '班级状态 WAITING/TEACHING/FINISHED',
    start_time DATETIME NOT NULL COMMENT '开班时间',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    student_count INT NOT NULL DEFAULT 0 COMMENT '班级人数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_campus_id (campus_id),
    KEY idx_head_teacher_id (head_teacher_id),
    KEY idx_course_id (course_id),
    KEY idx_class_status (class_status)
) COMMENT='班级表';

-- 班级课表表：存储班级每一天对应的课程安排
CREATE TABLE IF NOT EXISTS edu_class_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课表ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    course_detail_id BIGINT NULL COMMENT '课程详情ID，正式上课日才有值',
    course_day_num INT NULL COMMENT '课程第几天，正式上课日才有值',
    schedule_date DATE NOT NULL COMMENT '排课日期',
    schedule_type VARCHAR(32) NOT NULL DEFAULT 'CLASS' COMMENT '排课类型 CLASS/SELF_STUDY/REST',
    course_content VARCHAR(2000) NULL COMMENT '课程内容或当天安排说明',
    stage_name VARCHAR(100) NULL COMMENT '所属阶段快照，自习/休息可为空',
    teacher_id BIGINT NULL COMMENT '当天授课老师ID，仅上课日可能有值',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_class_schedule_date (class_id, schedule_date),
    UNIQUE KEY uk_class_course_day (class_id, course_day_num),
    KEY idx_class_id (class_id),
    KEY idx_course_id (course_id),
    KEY idx_teacher_id (teacher_id)
) COMMENT='班级课表表';

-- 作业发布表：存储老师面向班级发布的作业任务与答案信息
CREATE TABLE IF NOT EXISTS homework_assignment (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    title VARCHAR(200) NOT NULL COMMENT '作业标题',
    description TEXT NULL COMMENT '作业说明',
    class_id BIGINT UNSIGNED NOT NULL COMMENT '班级ID',
    homework_date DATE NULL COMMENT '作业日期',
    course_id BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
    course_detail_id BIGINT UNSIGNED NULL COMMENT '课程明细ID',
    course_content_snapshot VARCHAR(500) NULL COMMENT '发布时课程内容快照',
    attachment_name VARCHAR(255) NULL COMMENT '作业附件名称',
    attachment_url VARCHAR(500) NULL COMMENT '作业附件地址',
    attachment_type VARCHAR(50) NULL COMMENT '作业附件类型',
    answer_attachment_name VARCHAR(255) NULL COMMENT '答案附件名称',
    answer_attachment_url VARCHAR(500) NULL COMMENT '答案附件地址',
    answer_attachment_type VARCHAR(50) NULL COMMENT '答案附件类型',
    answer_publish_status VARCHAR(20) NOT NULL DEFAULT 'UNPUBLISHED' COMMENT '答案发布状态：UNPUBLISHED/PUBLISHED',
    answer_publish_time DATETIME NULL COMMENT '答案发布时间',
    answer_publish_teacher_id BIGINT UNSIGNED NULL COMMENT '答案发布老师ID',
    publish_teacher_id BIGINT UNSIGNED NOT NULL COMMENT '发布老师ID',
    publish_date DATETIME NOT NULL COMMENT '发布时间',
    deadline DATETIME NULL COMMENT '截止时间',
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED/CLOSED',
    allow_resubmit TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否允许重交',
    student_count_snapshot INT NOT NULL DEFAULT 0 COMMENT '发布时班级应交人数快照',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0否1是',
    close_time DATETIME NULL COMMENT '关闭时间',
    close_teacher_id BIGINT UNSIGNED NULL COMMENT '关闭老师ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_class_id (class_id),
    KEY idx_course_detail_id (course_detail_id),
    KEY idx_publish_teacher_id (publish_teacher_id),
    KEY idx_status (status),
    KEY idx_homework_date (homework_date)
) COMMENT='作业发布表';

-- 作业提交表：存储学员当前最新一次作业提交与老师批改结果
CREATE TABLE IF NOT EXISTS homework_submission (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    assignment_id BIGINT UNSIGNED NOT NULL COMMENT '作业ID',
    student_no VARCHAR(64) NOT NULL COMMENT '学员编号',
    student_name_snapshot VARCHAR(100) NULL COMMENT '学员姓名快照',
    submit_content TEXT NULL COMMENT '提交内容',
    attachment_name VARCHAR(255) NULL COMMENT '提交附件名称',
    attachment_url VARCHAR(500) NULL COMMENT '提交附件地址',
    attachment_type VARCHAR(50) NULL COMMENT '提交附件类型',
    submit_time DATETIME NULL COMMENT '提交时间',
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED/REVIEWED/RESUBMIT_REQUIRED',
    score DECIMAL(5,2) NULL COMMENT '得分',
    teacher_comment TEXT NULL COMMENT '老师评语',
    review_teacher_id BIGINT UNSIGNED NULL COMMENT '批改老师ID',
    review_time DATETIME NULL COMMENT '批改时间',
    version INT NOT NULL DEFAULT 1 COMMENT '提交版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_assignment_student (assignment_id, student_no),
    KEY idx_student_no (student_no),
    KEY idx_status (status),
    KEY idx_review_teacher_id (review_teacher_id)
) COMMENT='作业提交表';

-- 产品表：存储销售管理中的产品基础信息
CREATE TABLE IF NOT EXISTS sale_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '产品ID',
    product_code VARCHAR(64) NOT NULL COMMENT '产品编号',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    study_mode VARCHAR(32) NOT NULL COMMENT '学习方式：ONLINE/OFFLINE',
    product_price DECIMAL(10,2) NOT NULL COMMENT '产品价格',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_product_code (product_code)
) COMMENT='产品表';

-- 订单表：存储销售订单基础信息与金额快照
CREATE TABLE IF NOT EXISTS sale_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    student_name VARCHAR(64) NOT NULL COMMENT '学员姓名',
    phone VARCHAR(32) NOT NULL COMMENT '手机号',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    class_id BIGINT NULL COMMENT '班级ID，线下产品使用',
    student_code VARCHAR(64) NOT NULL COMMENT '学员编号',
    product_amount DECIMAL(10,2) NOT NULL COMMENT '产品金额',
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    receivable_amount DECIMAL(10,2) NOT NULL COMMENT '应收金额',
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '已付金额',
    order_status VARCHAR(32) NOT NULL COMMENT '订单状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_product_id (product_id),
    KEY idx_class_id (class_id),
    KEY idx_phone (phone),
    KEY idx_student_code (student_code),
    KEY idx_order_status (order_status)
) COMMENT='订单表';

CREATE TABLE IF NOT EXISTS sale_payment_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支付订单ID',
    payment_order_no VARCHAR(64) NOT NULL COMMENT '支付订单号',
    order_no VARCHAR(64) NOT NULL COMMENT '业务订单号',
    student_code VARCHAR(64) NOT NULL COMMENT '学员编号',
    student_name VARCHAR(64) NOT NULL COMMENT '学员姓名',
    phone VARCHAR(32) NOT NULL COMMENT '手机号',
    product_name VARCHAR(128) NOT NULL COMMENT '产品名称',
    payment_amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    payment_channel VARCHAR(32) NOT NULL COMMENT '支付渠道',
    payment_status VARCHAR(32) NOT NULL COMMENT '支付状态',
    unique_order_no VARCHAR(64) NULL COMMENT '易宝收款订单号',
    last_error_msg VARCHAR(255) NULL COMMENT '最近错误',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_payment_order_no (payment_order_no),
    KEY idx_order_no (order_no),
    KEY idx_payment_status (payment_status)
) COMMENT='支付订单表';

CREATE TABLE IF NOT EXISTS sale_payment_channel_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '渠道支付记录ID',
    payment_order_no VARCHAR(64) NULL COMMENT '支付订单号',
    unique_order_no VARCHAR(64) NOT NULL COMMENT '易宝收款订单号',
    merchant_no VARCHAR(64) NULL COMMENT '商户编号',
    order_id VARCHAR(64) NULL COMMENT '商户收款请求号',
    channel_order_id VARCHAR(128) NULL COMMENT '渠道订单号',
    order_amount DECIMAL(10,2) NULL COMMENT '订单金额',
    pay_amount DECIMAL(10,2) NULL COMMENT '支付金额',
    status VARCHAR(32) NULL COMMENT '渠道订单状态',
    pay_success_date VARCHAR(32) NULL COMMENT '支付成功时间',
    channel VARCHAR(32) NULL COMMENT '渠道类型',
    payer_info TEXT NULL COMMENT '付款信息',
    fail_code VARCHAR(64) NULL COMMENT '失败码',
    fail_reason VARCHAR(255) NULL COMMENT '失败原因',
    channel_trx_id VARCHAR(128) NULL COMMENT '渠道流水号',
    bank_order_id VARCHAR(128) NULL COMMENT '银行订单号',
    callback_type VARCHAR(128) NULL COMMENT '回调类型',
    callback_body TEXT NULL COMMENT '回调原文',
    callback_time DATETIME NULL COMMENT '回调时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_unique_order_no (unique_order_no),
    KEY idx_payment_order_no (payment_order_no),
    KEY idx_order_id (order_id)
) COMMENT='渠道支付回调记录表';

-- 学员表：存储学员基础资料、学习方式、班级和产品归属
CREATE TABLE IF NOT EXISTS edu_student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学员ID',
    student_code VARCHAR(64) NOT NULL COMMENT '学号',
    student_name VARCHAR(64) NOT NULL COMMENT '姓名',
    phone VARCHAR(32) NOT NULL COMMENT '手机号',
    password VARCHAR(128) NOT NULL DEFAULT '123456' COMMENT '登录密码',
    graduation_session INT NOT NULL COMMENT '毕业届数',
    school VARCHAR(128) NOT NULL COMMENT '学校',
    education VARCHAR(64) NOT NULL COMMENT '学历',
    study_mode VARCHAR(32) NOT NULL COMMENT '学习方式：ONLINE/OFFLINE',
    class_id BIGINT NULL COMMENT '班级ID，线下学员使用',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    join_time DATETIME NOT NULL COMMENT '加入时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_student_code (student_code),
    UNIQUE KEY uk_student_phone (phone),
    KEY idx_study_mode (study_mode),
    KEY idx_class_id (class_id),
    KEY idx_product_id (product_id)
) COMMENT='学员表';

-- 学生日常剪影表：存储学生自主提交的图片/视频学习生活素材
CREATE TABLE IF NOT EXISTS edu_student_daily_moment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    student_no VARCHAR(64) NOT NULL COMMENT '学员编号',
    title VARCHAR(255) NOT NULL COMMENT '素材标题',
    content_text VARCHAR(2000) NULL COMMENT '文字内容',
    file_url VARCHAR(3000) NULL COMMENT '文件访问URL（多个逗号分割）',
    record_time DATETIME NOT NULL COMMENT '素材时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    KEY idx_student_no (student_no),
    KEY idx_record_time (record_time)
) COMMENT='学生日常剪影表';

-- 初始化超级管理员角色
INSERT INTO sys_role (id, role_code, role_name, description, status)
VALUES (1, 'SUPER_ADMIN', '超级管理员', '拥有全部后台权限', 1)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description), status = VALUES(status);

-- 初始化默认管理员账号，默认密码为 password
INSERT INTO sys_user (id, username, password, nickname, real_name, phone, email, status)
VALUES (1, 'admin', 'password', '系统管理员', '超级管理员', '13800000000', 'admin@yq.com', 1)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), real_name = VALUES(real_name), phone = VALUES(phone), email = VALUES(email), status = VALUES(status);

-- 给管理员绑定超级管理员角色
INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 初始化认证与验签相关动态配置
INSERT INTO sys_config (k, v)
VALUES ('jwtSecretKey', 'yq-ai-admin-jwt-secret'),
       ('jwtExpireTime', '7200'),
       ('signSecretExpireSeconds', '7200'),
       ('signTimestampSkewSeconds', '300'),
       ('signNonceExpireSeconds', '300'),
       ('yeepayEnabled', 'false'),
       ('yeepayUseSandbox', 'false'),
       ('yeepayNotifyUrl', ''),
       ('yeepayReturnUrl', ''),
       ('yeepayExpireMinutes', '30'),
       ('yeepayLimitPayType', ''),
       ('studentAiSummaryTriggerUncompressedCount', '14'),
       ('studentAiSummaryKeepRecentCount', '6'),
       ('studentAiSummaryMaxTokens', '800')
ON DUPLICATE KEY UPDATE v = VALUES(v);

-- 权限初始化请继续执行：
-- src/main/resources/sql/init_permission_data.sql
