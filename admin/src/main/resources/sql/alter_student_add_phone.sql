ALTER TABLE edu_student
    ADD COLUMN phone VARCHAR(32) NOT NULL DEFAULT '' COMMENT '手机号' AFTER student_name,
    ADD UNIQUE KEY uk_student_phone (phone);
