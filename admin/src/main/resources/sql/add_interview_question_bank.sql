CREATE TABLE IF NOT EXISTS interview_question_bank (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    normalized_question VARCHAR(500) NOT NULL COMMENT '归一后的标准问题',
    category VARCHAR(100) DEFAULT NULL COMMENT '问题分类',
    tags VARCHAR(1000) DEFAULT NULL COMMENT '标签 JSON',
    standard_answer TEXT NOT NULL COMMENT '标准答案',
    short_answer TEXT DEFAULT NULL COMMENT '简短答案',
    follow_up_questions TEXT DEFAULT NULL COMMENT '追问列表 JSON',
    pitfalls TEXT DEFAULT NULL COMMENT '常见扣分点 JSON',
    source_count INT NOT NULL DEFAULT 0 COMMENT '出现次数',
    last_source_task_id BIGINT DEFAULT NULL COMMENT '最近来源任务 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_interview_question_normalized (normalized_question),
    KEY idx_interview_question_category (category),
    KEY idx_interview_question_updated_at (updated_at)
) COMMENT='面试问题题库';

CREATE TABLE IF NOT EXISTS interview_question_source (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    question_id BIGINT NOT NULL COMMENT '题库问题 ID',
    task_id BIGINT NOT NULL COMMENT '来源面试复盘任务 ID',
    original_question VARCHAR(1000) NOT NULL COMMENT '转写中的原始问题',
    answer_context TEXT DEFAULT NULL COMMENT '学生当时回答片段摘要',
    student_answer_quality VARCHAR(32) DEFAULT NULL COMMENT '学生回答质量：GOOD/PARTIAL/WRONG/INSUFFICIENT/UNKNOWN',
    student_answer_score INT DEFAULT NULL COMMENT '学生本次回答评分 0-100',
    student_answer_analysis TEXT DEFAULT NULL COMMENT '学生本次回答质量分析',
    improvement_suggestion TEXT DEFAULT NULL COMMENT '下次回答改进建议',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_interview_question_source (question_id, task_id, original_question(191)),
    KEY idx_interview_question_source_task (task_id),
    KEY idx_interview_question_source_question (question_id)
) COMMENT='面试问题来源记录';
