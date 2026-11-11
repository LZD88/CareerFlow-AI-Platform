# edu_course_detail

业务含义：课程详情表，存储课程每天的具体内容、所属阶段和第几天课。常用于分析课程阶段、课表进度和作业对应课程内容。

## 字段
- id: 课程详情ID，主键
- course_id: 课程ID，关联 edu_course.id
- stage_name: 所属阶段
- day_num: 第几天课
- course_content: 课程内容
- created_at: 创建时间
- updated_at: 更新时间

## 禁止查询字段
- 无

## 索引
- PRIMARY(id)
- idx_course_id(course_id)
- idx_stage_day(stage_name, day_num)

## 关联
- edu_course_detail.course_id -> edu_course.id
- edu_class_schedule.course_detail_id -> edu_course_detail.id
- homework_assignment.course_detail_id -> edu_course_detail.id

## 常用口径
- 课程详情数 = COUNT(DISTINCT edu_course_detail.id)

## 查询约束
- 查询课程详情必须带 LIMIT
- 按课程查询时优先使用 course_id
- 按阶段和天数查询时优先使用 stage_name、day_num
