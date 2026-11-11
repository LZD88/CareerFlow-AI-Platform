ALTER TABLE sale_order
    ADD COLUMN class_id BIGINT NULL COMMENT '班级ID，线下产品使用' AFTER product_id,
    ADD KEY idx_class_id (class_id);
