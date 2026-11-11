-- 知识库文档管理

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    document_code VARCHAR(128) NOT NULL COMMENT '传给向量库的业务文档ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    content MEDIUMTEXT NOT NULL COMMENT '文档正文',
    source_type VARCHAR(40) NOT NULL DEFAULT 'manual' COMMENT 'manual / markdown / txt / generated_java_md / pdf / word',
    source_path VARCHAR(500) NOT NULL DEFAULT '' COMMENT '来源路径',
    course_id BIGINT NULL COMMENT '课程ID',
    class_id BIGINT NULL COMMENT '班级ID',
    study_mode VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '适用学员范围：ALL / ONLINE / OFFLINE',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    permission_scope VARCHAR(40) NOT NULL DEFAULT 'course' COMMENT '权限范围',
    status VARCHAR(30) NOT NULL DEFAULT 'INDEXING' COMMENT 'PENDING / INDEXING / INDEXED / FAILED / DISABLED',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '入库chunk数量',
    error_message VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '失败原因',
    indexed_at DATETIME NULL COMMENT '最近入库成功时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_knowledge_document_code (document_code),
    INDEX idx_knowledge_document_status (status, updated_at),
    INDEX idx_knowledge_document_course_scope (course_id, study_mode)
) COMMENT='知识库文档元数据表';

-- 如果你的库里已经存在旧版 knowledge_document 表，需要手动执行下面这句迁移：
-- ALTER TABLE knowledge_document ADD COLUMN study_mode VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '适用学员范围：ALL / ONLINE / OFFLINE';

-- 注意：这里不要手写 sys_permission.id。
-- 线上库的 56、57、58 等 ID 可能已经被其他权限占用，写死主键再配合 ON DUPLICATE KEY UPDATE 会覆盖旧权限。
-- 正确做法是让 MySQL 自增生成 ID，用唯一的 permission_code 做幂等更新。
INSERT INTO sys_permission (parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (101, 'menu:system:knowledge-document', '知识库管理', 'MENU', null, 6, '系统配置下的知识库文档管理菜单', 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    sort_num = VALUES(sort_num),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO sys_permission (parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (101, 'menu:system:code-knowledge', '代码知识库', 'MENU', null, 7, '系统配置下的代码知识库管理菜单', 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    sort_num = VALUES(sort_num),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO sys_permission (parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:knowledge-document') knowledge_menu),
     'knowledge-document:list', '知识库文档列表', 'API', '/api/knowledge/documents', 56, '查询知识库文档列表', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:knowledge-document') knowledge_menu),
     'knowledge-document:create', '新增知识库文档', 'API', '/api/knowledge/documents', 57, '新增并入库知识库文档', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:knowledge-document') knowledge_menu),
     'knowledge-document:upload-md', '批量上传Markdown知识文档', 'API', '/api/knowledge/documents/upload-md', 57, '批量上传 Markdown 知识文档，可选择是否立即写入 Milvus', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:knowledge-document') knowledge_menu),
     'knowledge-document:upload-json', '批量上传JSON知识文档', 'API', '/api/knowledge/documents/upload-json', 58, '批量上传 JSON 指标知识文档，可选择是否立即写入 Milvus', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:knowledge-document') knowledge_menu),
     'knowledge-document:update', '更新知识库文档', 'API', '/api/knowledge/documents/*/update', 59, '更新并重新入库知识库文档', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:knowledge-document') knowledge_menu),
     'knowledge-document:detail', '知识库文档详情', 'API', '/api/knowledge/documents/*', 60, '查询知识库文档详情', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:knowledge-document') knowledge_menu),
     'knowledge-document:chunks', '知识库文档Chunk详情', 'API', '/api/knowledge/documents/*/chunks', 61, '查询知识库文档向量切片详情', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:knowledge-document') knowledge_menu),
     'knowledge-document:reindex', '重新入库知识库文档', 'API', '/api/knowledge/documents/*/reindex', 62, '重新入库知识库文档', 1),
    ((SELECT id FROM (SELECT id FROM sys_permission WHERE permission_code = 'menu:system:knowledge-document') knowledge_menu),
     'knowledge-document:disable', '禁用知识库文档', 'API', '/api/knowledge/documents/*/disable', 63, '禁用知识库文档', 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    permission_name = VALUES(permission_name),
    permission_type = VALUES(permission_type),
    api_path = VALUES(api_path),
    sort_num = VALUES(sort_num),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id
FROM sys_permission
WHERE permission_code IN (
    'menu:system:knowledge-document',
    'menu:system:code-knowledge',
    'knowledge-document:list',
    'knowledge-document:create',
    'knowledge-document:upload-md',
    'knowledge-document:upload-json',
    'knowledge-document:update',
    'knowledge-document:detail',
    'knowledge-document:chunks',
    'knowledge-document:reindex',
    'knowledge-document:disable'
)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
