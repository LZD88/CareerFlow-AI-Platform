ALTER TABLE sale_order
    ADD COLUMN phone VARCHAR(32) NOT NULL DEFAULT '' COMMENT '手机号' AFTER student_name,
    ADD KEY idx_phone (phone);
