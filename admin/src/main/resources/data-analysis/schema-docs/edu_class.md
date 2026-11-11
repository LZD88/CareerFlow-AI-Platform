# edu_class

业务含义：班级表，存储班级期数、校区、班主任、班级状态、开班时间、课程和班级人数。常用于班级维度统计和教学运营分析。

## 字段
- id: 班级ID，主键
- class_term: 班级期数
- campus_id: 校区ID，关联 edu_campus.id
- head_teacher_id: 班主任用户ID，关联 sys_user.id
- class_status: 班级状态，WAITING待开班，TEACHING授课中，FINISHED已结课
- start_time: 开班时间
- course_id: 课程ID，关联 edu_course.id
- student_count: 班级人数
- created_at: 创建时间
- updated_at: 更新时间

## 禁止查询字段
- 无

## 索引
- PRIMARY(id)
- idx_campus_id(campus_id)
- idx_head_teacher_id(head_teacher_id)
- idx_course_id(course_id)
- idx_class_status(class_status)

## 关联
- edu_class.campus_id -> edu_campus.id
- edu_class.course_id -> edu_course.id
- edu_class.head_teacher_id -> sys_user.id
- edu_student.class_id -> edu_class.id
- sale_order.class_id -> edu_class.id
- homework_assignment.class_id -> edu_class.id

## 常用口径
- 班级数 = COUNT(DISTINCT edu_class.id)
- 在读班级数 = COUNT(DISTINCT edu_class.id)，过滤 edu_class.class_status = TEACHING
- 班级人数 = SUM(edu_class.student_count)

## 查询约束
- 查询 edu_class 必须带 LIMIT
- 大范围统计优先使用 campus_id、course_id、head_teacher_id、class_status 过滤
