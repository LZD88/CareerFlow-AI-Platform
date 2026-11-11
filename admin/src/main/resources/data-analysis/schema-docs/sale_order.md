# sale_order

业务含义：订单表，存储销售订单基础信息、学员快照、产品、班级、金额和订单状态。常用于统计订单数、应收金额、已付金额、报名订单和按产品/班级/学员维度分析销售情况。

## 字段
- id: 订单ID，主键
- order_no: 订单编号，可用于关联 sale_payment_order.order_no
- student_name: 学员姓名快照
- product_id: 产品ID，关联 sale_product.id
- class_id: 班级ID，线下产品使用，关联 edu_class.id
- student_code: 学员编号，关联 edu_student.student_code
- product_amount: 产品金额
- discount_amount: 优惠金额
- receivable_amount: 应收金额
- paid_amount: 已付金额
- order_status: 订单状态
- created_at: 创建时间
- updated_at: 更新时间

## 禁止查询字段
- phone: 手机号，敏感字段，默认禁止查询和展示

## 索引
- PRIMARY(id)
- uk_order_no(order_no)
- idx_product_id(product_id)
- idx_class_id(class_id)
- idx_phone(phone)
- idx_student_code(student_code)
- idx_order_status(order_status)

## 关联
- sale_order.product_id -> sale_product.id
- sale_order.class_id -> edu_class.id
- sale_order.student_code -> edu_student.student_code
- sale_payment_order.order_no -> sale_order.order_no

## 常用口径
- 订单数 = COUNT(DISTINCT sale_order.id)
- 应收金额 = SUM(sale_order.receivable_amount)
- 已付金额 = SUM(sale_order.paid_amount)
- 优惠金额 = SUM(sale_order.discount_amount)
- 未付金额 = SUM(sale_order.receivable_amount - sale_order.paid_amount)

## 查询约束
- 查询 sale_order 必须带 LIMIT
- 大范围统计优先使用 order_status、product_id、class_id、student_code 等索引字段过滤
- 查询手机号相关条件可以用于内部精确过滤，但不能展示 phone 字段
- 如果问题涉及实际支付成功金额，优先结合 sale_payment_order.payment_status 和 sale_payment_order.payment_amount 判断
