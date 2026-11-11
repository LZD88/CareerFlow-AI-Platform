# sale_payment_order

业务含义：支付订单表，存储支付订单号、业务订单号、学员快照、产品快照、支付金额、支付渠道和支付状态。常用于统计支付金额、支付订单数和支付成功情况。

## 字段
- id: 支付订单ID，主键
- payment_order_no: 支付订单号
- order_no: 业务订单号，关联 sale_order.order_no
- student_code: 学员编号，关联 edu_student.student_code
- student_name: 学员姓名快照
- product_name: 产品名称快照
- payment_amount: 支付金额
- payment_channel: 支付渠道
- payment_status: 支付状态
- unique_order_no: 易宝收款订单号
- created_at: 创建时间
- updated_at: 更新时间

## 禁止查询字段
- phone: 手机号，敏感字段，默认禁止查询和展示
- last_error_msg: 最近错误信息，一期不作为默认分析字段

## 索引
- PRIMARY(id)
- uk_payment_order_no(payment_order_no)
- idx_order_no(order_no)
- idx_payment_status(payment_status)

## 关联
- sale_payment_order.order_no -> sale_order.order_no
- sale_payment_order.student_code -> edu_student.student_code
- sale_payment_channel_order.payment_order_no -> sale_payment_order.payment_order_no

## 常用口径
- 支付订单数 = COUNT(DISTINCT sale_payment_order.id)
- 支付金额 = SUM(sale_payment_order.payment_amount)，通常过滤 sale_payment_order.payment_status = SUCCESS
- 支付成功订单数 = COUNT(DISTINCT sale_payment_order.id)，过滤 sale_payment_order.payment_status = SUCCESS

## 查询约束
- 查询 sale_payment_order 必须带 LIMIT
- 大范围统计优先使用 payment_status、order_no、payment_order_no 过滤
- 不允许展示 phone 字段
