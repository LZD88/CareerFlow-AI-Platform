-- 产品管理表与权限初始化脚本
-- 适用于已执行过 init.sql 的存量环境

CREATE TABLE IF NOT EXISTS sale_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '产品ID',
    product_code VARCHAR(64) NOT NULL COMMENT '产品编号',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    study_mode VARCHAR(32) NOT NULL COMMENT '学习方式：ONLINE/OFFLINE',
    product_price DECIMAL(10,2) NOT NULL COMMENT '产品价格',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_product_code (product_code)
) COMMENT='产品表';

INSERT INTO sys_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (113, 0, 'menu:sales', '销售管理', 'MENU', null, 3, '左侧导航中的销售管理父级菜单', 1),
    (114, 113, 'menu:sales:product', '产品管理', 'MENU', null, 1, '销售管理下的产品管理菜单', 1),
    (237, 114, 'button:product:create', '产品新增按钮', 'BUTTON', null, 1, '产品管理页面的新增按钮', 1),
    (238, 114, 'button:product:update', '产品编辑按钮', 'BUTTON', null, 2, '产品管理页面的编辑按钮', 1),
    (239, 114, 'button:product:delete', '产品删除按钮', 'BUTTON', null, 3, '产品管理页面的删除按钮', 1),
    (46, 114, 'product:view', '查看产品', 'API', '/api/sales/products/**', 11, '查看产品列表与详情接口', 1),
    (47, 114, 'product:create', '创建产品', 'API', '/api/sales/products', 12, '新增产品接口', 1),
    (48, 114, 'product:update', '编辑产品', 'API', '/api/sales/products/*', 13, '编辑产品接口', 1),
    (49, 114, 'product:delete', '删除产品', 'API', '/api/sales/products/*', 14, '删除产品接口', 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), permission_type = VALUES(permission_type), api_path = VALUES(api_path), description = VALUES(description), status = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 46), (1, 47), (1, 48), (1, 49), (1, 113), (1, 114), (1, 237), (1, 238), (1, 239)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
