# sale_product

业务含义：产品表，存储销售产品编号、产品名称、学习方式和价格。常用于按产品统计订单、学员、销售和支付数据。

## 字段
- id: 产品ID，主键
- product_code: 产品编号
- product_name: 产品名称
- study_mode: 学习方式，ONLINE线上，OFFLINE线下
- product_price: 产品价格
- created_at: 创建时间
- updated_at: 更新时间

## 禁止查询字段
- 无

## 索引
- PRIMARY(id)
- uk_product_code(product_code)

## 关联
- sale_order.product_id -> sale_product.id
- edu_student.product_id -> sale_product.id

## 常用口径
- 产品数 = COUNT(DISTINCT sale_product.id)
- 产品标价总额 = SUM(sale_product.product_price)

## 查询约束
- 查询 sale_product 明细必须带 LIMIT
