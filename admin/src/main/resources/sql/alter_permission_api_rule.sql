-- API权限规则增量脚本
-- 适用于已经存在 sys_permission 表但还没有接口路径字段的数据库

ALTER TABLE sys_permission
    ADD COLUMN api_path VARCHAR(255) NULL COMMENT 'API路径匹配规则，仅API权限使用' AFTER permission_type;
