# edu_campus

业务含义：校区表，存储校区名称、负责人、地址、联系电话、状态等信息。常用于按校区统计班级、学员、订单和支付数据。

## 字段
- id: 校区ID，主键
- campus_name: 校区名称
- principal_user_id: 负责人用户ID，关联 sys_user.id
- address: 校区地址
- remark: 备注
- status: 状态，1启用，0禁用
- created_at: 创建时间
- updated_at: 更新时间

## 禁止查询字段
- contact_phone: 校区联系电话，敏感字段，默认禁止展示

## 索引
- PRIMARY(id)
- idx_principal_user_id(principal_user_id)

## 关联
- edu_class.campus_id -> edu_campus.id
- edu_campus.principal_user_id -> sys_user.id

## 常用口径
- 校区数 = COUNT(DISTINCT edu_campus.id)
- 启用校区数 = COUNT(DISTINCT edu_campus.id)，过滤 edu_campus.status = 1

## 查询约束
- 查询校区明细必须带 LIMIT
- 不允许展示 contact_phone
