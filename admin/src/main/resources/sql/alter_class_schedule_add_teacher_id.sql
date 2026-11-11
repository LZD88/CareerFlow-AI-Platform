ALTER TABLE edu_class_schedule
    ADD COLUMN teacher_id BIGINT NULL COMMENT '当天授课老师ID，仅上课日可能有值' AFTER stage_name,
    ADD KEY idx_teacher_id (teacher_id);
