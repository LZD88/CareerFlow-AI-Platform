-- 权限树结构增量脚本
-- 适用于已经执行过旧版 init.sql 的数据库

ALTER TABLE sys_permission
    ADD COLUMN parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父权限ID，根节点为0' AFTER id;

ALTER TABLE sys_permission
    ADD COLUMN sort_num INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前' AFTER permission_type;
