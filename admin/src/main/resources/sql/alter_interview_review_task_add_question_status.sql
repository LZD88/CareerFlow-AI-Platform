ALTER TABLE interview_review_task
    ADD COLUMN question_status VARCHAR(32) NOT NULL DEFAULT 'NONE' COMMENT '题库生成状态：NONE 未生成 / PROCESSING 生成中 / DONE 已生成 / FAILED 失败' AFTER fail_reason,
    ADD COLUMN question_extracted_count INT NOT NULL DEFAULT 0 COMMENT '题库抽取问题数' AFTER question_status,
    ADD COLUMN question_created_count INT NOT NULL DEFAULT 0 COMMENT '题库新增问题数' AFTER question_extracted_count,
    ADD COLUMN question_merged_count INT NOT NULL DEFAULT 0 COMMENT '题库合并问题数' AFTER question_created_count,
    ADD COLUMN question_fail_reason TEXT DEFAULT NULL COMMENT '题库生成失败原因' AFTER question_merged_count,
    ADD KEY idx_question_status (question_status);
