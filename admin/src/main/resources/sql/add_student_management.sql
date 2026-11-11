-- 学员管理表初始化脚本
-- 适用于已执行过 init.sql 的存量环境

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
