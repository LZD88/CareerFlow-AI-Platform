ALTER TABLE interview_question_source
    ADD COLUMN student_answer_quality VARCHAR(32) DEFAULT NULL COMMENT '学生回答质量：GOOD/PARTIAL/WRONG/INSUFFICIENT/UNKNOWN' AFTER answer_context,
    ADD COLUMN student_answer_score INT DEFAULT NULL COMMENT '学生本次回答评分 0-100' AFTER student_answer_quality,
    ADD COLUMN student_answer_analysis TEXT DEFAULT NULL COMMENT '学生本次回答质量分析' AFTER student_answer_score,
    ADD COLUMN improvement_suggestion TEXT DEFAULT NULL COMMENT '下次回答改进建议' AFTER student_answer_analysis;
