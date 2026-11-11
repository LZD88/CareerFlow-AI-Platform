# edu_student

业务含义：学员表，存储学员学号、姓名、学校、学历、学习方式、班级、产品和加入时间。常用于统计学员数、新增学员、线上线下学员和班级学员构成。

## 字段
- id: 学员ID，主键
- student_code: 学号，业务学员编号
- student_name: 姓名
- graduation_session: 毕业届数
- school: 学校
- education: 学历
- study_mode: 学习方式，ONLINE线上，OFFLINE线下
- class_id: 班级ID，线下学员使用，关联 edu_class.id
- product_id: 产品ID，关联 sale_product.id
- join_time: 加入时间
- created_at: 创建时间
- updated_at: 更新时间

## 禁止查询字段
- phone: 手机号，敏感字段，默认禁止查询和展示
- password: 登录密码，禁止查询

## 索引
- PRIMARY(id)
- uk_student_code(student_code)
- uk_student_phone(phone)
- idx_study_mode(study_mode)
- idx_class_id(class_id)
- idx_product_id(product_id)

## 关联
- edu_student.class_id -> edu_class.id
- edu_student.product_id -> sale_product.id
- sale_order.student_code -> edu_student.student_code
- sale_payment_order.student_code -> edu_student.student_code
- homework_submission.student_no -> edu_student.student_code
- edu_student_daily_moment.student_no -> edu_student.student_code

## 常用口径
- 学员数 = COUNT(DISTINCT edu_student.id)
- 新增学员数 = COUNT(DISTINCT edu_student.id)，按 edu_student.join_time 统计
- 线上学员数 = COUNT(DISTINCT edu_student.id)，过滤 edu_student.study_mode = ONLINE
- 线下学员数 = COUNT(DISTINCT edu_student.id)，过滤 edu_student.study_mode = OFFLINE

## 查询约束
- 查询 edu_student 必须带 LIMIT
- 大范围统计优先使用 study_mode、class_id、product_id、student_code 过滤
- 不允许展示 phone、password 字段
