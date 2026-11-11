-- 学生日常剪影表
-- 适用于已执行过 init.sql 的存量环境

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
