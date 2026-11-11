ALTER TABLE edu_class
    ADD COLUMN campus_id BIGINT NOT NULL DEFAULT 1 COMMENT '校区ID' AFTER class_term,
    ADD KEY idx_campus_id (campus_id);
