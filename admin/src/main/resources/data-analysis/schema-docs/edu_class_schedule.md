# edu_class_schedule

业务含义：班级课表表，存储班级每天的排课日期、排课类型、课程内容、课程第几天和授课老师。常用于课表、课程进度和老师授课安排分析。

## 字段
- id: 课表ID，主键
- class_id: 班级ID，关联 edu_class.id
- course_id: 课程ID，关联 edu_course.id
- course_detail_id: 课程详情ID，关联 edu_course_detail.id
- course_day_num: 课程第几天
- schedule_date: 排课日期
- schedule_type: 排课类型，CLASS上课，SELF_STUDY自习，REST休息
- course_content: 课程内容或当天安排说明
- stage_name: 所属阶段快照
- teacher_id: 当天授课老师ID，关联 sys_user.id
- created_at: 创建时间
- updated_at: 更新时间

## 禁止查询字段
- 无

## 索引
- PRIMARY(id)
- uk_class_schedule_date(class_id, schedule_date)
- uk_class_course_day(class_id, course_day_num)
- idx_class_id(class_id)
- idx_course_id(course_id)
- idx_teacher_id(teacher_id)

## 关联
- edu_class_schedule.class_id -> edu_class.id
- edu_class_schedule.course_id -> edu_course.id
- edu_class_schedule.course_detail_id -> edu_course_detail.id
- edu_class_schedule.teacher_id -> sys_user.id

## 常用口径
- 排课天数 = COUNT(DISTINCT edu_class_schedule.id)
- 上课天数 = COUNT(DISTINCT edu_class_schedule.id)，过滤 edu_class_schedule.schedule_type = CLASS

## 查询约束
- 查询 edu_class_schedule 必须带 LIMIT
- 按班级和日期查课表优先使用 class_id、schedule_date
- 按老师查课表优先使用 teacher_id
