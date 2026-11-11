-- 自媒体 Agent 草稿表

CREATE TABLE IF NOT EXISTS media_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    theme VARCHAR(255) NOT NULL COMMENT '创作主题',
    topic_json JSON NOT NULL COMMENT '用户选中的选题完整 JSON',
    topic_title VARCHAR(500) NOT NULL COMMENT '选题方向标题，便于列表展示',
    style VARCHAR(100) DEFAULT NULL COMMENT '内容风格',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT 草稿 / ARCHIVED 已归档',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_created_at (created_at),
    KEY idx_created_by (created_by),
    KEY idx_topic_title (topic_title(191))
) COMMENT='自媒体创作项目';

CREATE TABLE IF NOT EXISTS media_draft (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    project_id BIGINT NOT NULL COMMENT '创作项目 ID',
    content_type VARCHAR(32) NOT NULL COMMENT '内容类型：xiaohongshu_note / douyin_video / douyin_image_text',
    title_options JSON DEFAULT NULL COMMENT '标题候选或开场钩子 JSON 数组',
    cover_text VARCHAR(255) DEFAULT NULL COMMENT '封面文案',
    body MEDIUMTEXT DEFAULT NULL COMMENT '正文、口播脚本或图文说明',
    image_page_plan JSON DEFAULT NULL COMMENT '图片页结构建议 JSON 数组',
    shot_list JSON DEFAULT NULL COMMENT '短视频分镜建议 JSON 数组',
    subtitles JSON DEFAULT NULL COMMENT '短视频屏幕字幕 JSON 数组',
    hashtags JSON DEFAULT NULL COMMENT '话题标签 JSON 数组',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT 草稿 / READY 已确认 / ARCHIVED 已归档',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_project_content_version (project_id, content_type, version),
    KEY idx_project_id (project_id),
    KEY idx_content_type (content_type),
    KEY idx_created_at (created_at)
) COMMENT='自媒体文字草稿';
