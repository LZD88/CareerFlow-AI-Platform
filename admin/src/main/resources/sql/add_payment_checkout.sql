-- 可重复执行，不改变已有演示支付。
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sale_payment_order' AND column_name='cashier_url')=0,
 'ALTER TABLE sale_payment_order ADD COLUMN cashier_url TEXT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sale_payment_order' AND column_name='expires_at')=0,
 'ALTER TABLE sale_payment_order ADD COLUMN expires_at DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sale_payment_order' AND column_name='merchant_no')=0,
 'ALTER TABLE sale_payment_order ADD COLUMN merchant_no VARCHAR(64) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_permission(parent_id,permission_code,permission_name,permission_type,api_path,sort_num,description,status)
SELECT id,'button:order:collect','在线收款','BUTTON',NULL,5,'通过易宝收银台收取报名订单剩余款项',1
FROM sys_permission WHERE permission_code='menu:sales:order'
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name),status=1;
INSERT INTO sys_permission(parent_id,permission_code,permission_name,permission_type,api_path,sort_num,description,status)
SELECT id,'order:collect','订单收银台接口','API','/api/payments/orders/*/checkout',21,'查询和创建易宝收银台',1
FROM sys_permission WHERE permission_code='menu:sales:order'
ON DUPLICATE KEY UPDATE api_path=VALUES(api_path),status=1;
INSERT IGNORE INTO sys_role_permission(role_id,permission_id)
SELECT DISTINCT rp.role_id,target.id FROM sys_role_permission rp
JOIN sys_permission source ON source.id=rp.permission_id CROSS JOIN sys_permission target
WHERE source.permission_code IN ('button:order:create','button:order:update')
AND target.permission_code IN ('button:order:collect','order:collect');
