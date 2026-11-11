ALTER TABLE interview_review_task
    ADD COLUMN resume_filename VARCHAR(255) DEFAULT NULL COMMENT '简历原始文件名' AFTER audio_size,
    ADD COLUMN resume_object_key VARCHAR(500) DEFAULT NULL COMMENT '简历 TOS 对象 Key' AFTER resume_filename,
    ADD COLUMN resume_url VARCHAR(1000) DEFAULT NULL COMMENT '简历文件访问地址' AFTER resume_object_key,
    ADD COLUMN resume_content_type VARCHAR(100) DEFAULT NULL COMMENT '简历 Content-Type' AFTER resume_url,
    ADD COLUMN resume_size BIGINT DEFAULT NULL COMMENT '简历文件大小，单位字节' AFTER resume_content_type;
