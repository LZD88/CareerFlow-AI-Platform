ALTER TABLE sys_user
    ADD COLUMN union_id VARCHAR(128) NULL UNIQUE COMMENT '飞书 union_id' AFTER email;
