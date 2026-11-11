CREATE TABLE IF NOT EXISTS sale_payment_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支付订单ID',
    payment_order_no VARCHAR(64) NOT NULL COMMENT '支付订单号',
    order_no VARCHAR(64) NOT NULL COMMENT '业务订单号',
    student_code VARCHAR(64) NOT NULL COMMENT '学员编号',
    student_name VARCHAR(64) NOT NULL COMMENT '学员姓名',
    phone VARCHAR(32) NOT NULL COMMENT '手机号',
    product_name VARCHAR(128) NOT NULL COMMENT '产品名称',
    payment_amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    payment_channel VARCHAR(32) NOT NULL COMMENT '支付渠道',
    payment_status VARCHAR(32) NOT NULL COMMENT '支付状态',
    unique_order_no VARCHAR(64) NULL COMMENT '易宝收款订单号',
    last_error_msg VARCHAR(255) NULL COMMENT '最近错误',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_payment_order_no (payment_order_no),
    KEY idx_order_no (order_no),
    KEY idx_payment_status (payment_status)
) COMMENT='支付订单表';

INSERT INTO sys_permission (id, parent_id, permission_code, permission_name, permission_type, api_path, sort_num, description, status)
VALUES
    (122, 113, 'menu:sales:payment-order', '支付订单查询', 'MENU', null, 3, '销售管理下的支付订单查询菜单', 1),
    (72, 122, 'payment-order:view', '查看支付订单', 'API', '/api/sales/payment-orders/**', 31, '查看支付订单列表接口', 1)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name), permission_type = VALUES(permission_type), api_path = VALUES(api_path), description = VALUES(description), status = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
    (1, 72), (1, 122)
ON DUPLICATE KEY UPDATE permission_id = VALUES(permission_id);
