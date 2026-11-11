-- 值班模块的基础表；班级与全局值班共用，class_id=0 表示全局值班。
CREATE TABLE IF NOT EXISTS edu_duty_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    duty_date DATE NOT NULL,
    duty_type VARCHAR(40) NOT NULL,
    class_id BIGINT NOT NULL DEFAULT 0,
    start_time VARCHAR(8) NOT NULL,
    end_time VARCHAR(8) NOT NULL,
    teacher_id BIGINT NULL,
    remark VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_duty_day_type_class (duty_date, duty_type, class_id),
    KEY idx_duty_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学值班安排';
