-- 订单管理表与权限初始化脚本
-- 适用于已执行过 init.sql 的存量环境

CREATE TABLE IF NOT EXISTS sale_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    student_name VARCHAR(64) NOT NULL COMMENT '学员姓名',
    phone VARCHAR(32) NOT NULL COMMENT '手机号',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    class_id BIGINT NULL COMMENT '班级ID，线下产品使用',
    student_code VARCHAR(64) NOT NULL COMMENT '学员编号',
    product_amount DECIMAL(10,2) NOT NULL COMMENT '产品金额',
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    receivable_amount DECIMAL(10,2) NOT NULL COMMENT '应收金额',
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '已付金额',
    order_status VARCHAR(32) NOT NULL COMMENT '订单状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_product_id (product_id),
    KEY idx_class_id (class_id),
    KEY idx_phone (phone),
    KEY idx_student_code (student_code),
    KEY idx_order_status (order_status)
) COMMENT='订单表';

INSERT INTO sys_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (115, 113, 'menu:sales:order', '订单管理', 'MENU', null, 2, '销售管理下的订单管理菜单', 1),
    (240, 115, 'button:order:create', '订单新增按钮', 'BUTTON', null, 1, '订单管理页面的新增按钮', 1),
    (241, 115, 'button:order:update', '订单编辑按钮', 'BUTTON', null, 2, '订单管理页面的编辑按钮', 1),
    (242, 115, 'button:order:delete', '订单删除按钮', 'BUTTON', null, 3, '订单管理页面的删除按钮', 1),
    (50, 115, 'order:view', '查看订单', 'API', '/api/sales/orders/**', 15, '查看订单列表与详情接口', 1),
    (51, 115, 'order:create', '创建订单', 'API', '/api/sales/orders', 16, '新增订单接口', 1),
    (52, 115, 'order:update', '编辑订单', 'API', '/api/sales/orders/*', 17, '编辑订单接口', 1),
    (53, 115, 'order:delete', '删除订单', 'API', '/api/sales/orders/*', 18, '删除订单接口', 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), permission_type = VALUES(permission_type), api_path = VALUES(api_path), description = VALUES(description), status = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 50), (1, 51), (1, 52), (1, 53), (1, 115), (1, 240), (1, 241), (1, 242)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
