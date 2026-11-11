# sale_payment_channel_order

业务含义：渠道支付回调记录表，存储第三方支付渠道回调、渠道流水、订单金额、支付金额、渠道状态、失败原因和回调时间。常用于排查支付渠道问题和渠道支付统计。

## 字段
- id: 渠道支付记录ID，主键
- payment_order_no: 支付订单号，关联 sale_payment_order.payment_order_no
- unique_order_no: 易宝收款订单号
- order_id: 商户收款请求号
- channel_order_id: 渠道订单号
- order_amount: 订单金额
- pay_amount: 支付金额
- status: 渠道订单状态
- pay_success_date: 支付成功时间，字符串格式
- channel: 渠道类型
- fail_code: 失败码
- fail_reason: 失败原因
- channel_trx_id: 渠道流水号
- callback_type: 回调类型
- callback_time: 回调时间
- created_at: 创建时间
- updated_at: 更新时间

## 禁止查询字段
- merchant_no: 商户编号，敏感字段
- payer_info: 付款信息，敏感字段
- bank_order_id: 银行订单号，敏感字段
- callback_body: 回调原文，敏感字段，禁止查询和展示

## 索引
- PRIMARY(id)
- uk_unique_order_no(unique_order_no)
- idx_payment_order_no(payment_order_no)
- idx_order_id(order_id)

## 关联
- sale_payment_channel_order.payment_order_no -> sale_payment_order.payment_order_no

## 常用口径
- 渠道支付记录数 = COUNT(DISTINCT sale_payment_channel_order.id)
- 渠道支付金额 = SUM(sale_payment_channel_order.pay_amount)
- 渠道订单金额 = SUM(sale_payment_channel_order.order_amount)

## 查询约束
- 查询 sale_payment_channel_order 必须带 LIMIT
- 大范围查询优先使用 payment_order_no、order_id、unique_order_no 过滤
- 不允许展示 callback_body、payer_info 等敏感字段
