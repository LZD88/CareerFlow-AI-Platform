# edu_course

业务含义：课程表，存储课程名称、课程天数、飞书资料路径和课程状态。常用于按课程统计班级、学员、课表、作业和销售情况。

## 字段
- id: 课程ID，主键
- course_name: 课程名称
- course_days: 课程天数
- status: 状态，1启用，0禁用
- created_at: 创建时间
- updated_at: 更新时间

## 禁止查询字段
- feishu_material_path: 飞书资料路径，一期不用于数据分析展示

## 索引
- PRIMARY(id)

## 关联
- edu_course_detail.course_id -> edu_course.id
- edu_class.course_id -> edu_course.id
- edu_class_schedule.course_id -> edu_course.id
- homework_assignment.course_id -> edu_course.id

## 常用口径
- 课程数 = COUNT(DISTINCT edu_course.id)
- 启用课程数 = COUNT(DISTINCT edu_course.id)，过滤 edu_course.status = 1

## 查询约束
- 查询课程明细必须带 LIMIT
