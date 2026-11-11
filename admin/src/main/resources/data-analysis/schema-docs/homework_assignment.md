# homework_assignment

业务含义：作业发布表，存储老师面向班级发布的作业任务、作业日期、课程、发布时间、截止时间、状态和答案发布状态。常用于统计作业发布情况。

## 字段
- id: 作业ID，主键
- title: 作业标题
- description: 作业说明
- class_id: 班级ID，关联 edu_class.id
- homework_date: 作业日期
- course_id: 课程ID，关联 edu_course.id
- course_detail_id: 课程明细ID，关联 edu_course_detail.id
- course_content_snapshot: 发布时课程内容快照
- answer_publish_status: 答案发布状态，UNPUBLISHED未发布，PUBLISHED已发布
- publish_teacher_id: 发布老师ID，关联 sys_user.id
- publish_date: 发布时间
- deadline: 截止时间
- status: 作业状态，PUBLISHED已发布，CLOSED已关闭
- allow_resubmit: 是否允许重交
- student_count_snapshot: 发布时班级应交人数快照
- deleted: 是否删除，0否1是
- create_time: 创建时间
- update_time: 更新时间

## 禁止查询字段
- attachment_url: 作业附件地址，一期不作为默认分析字段
- answer_attachment_url: 答案附件地址，一期不作为默认分析字段

## 索引
- PRIMARY(id)
- idx_class_id(class_id)
- idx_course_detail_id(course_detail_id)
- idx_publish_teacher_id(publish_teacher_id)
- idx_status(status)
- idx_homework_date(homework_date)

## 关联
- homework_assignment.class_id -> edu_class.id
- homework_assignment.course_id -> edu_course.id
- homework_assignment.course_detail_id -> edu_course_detail.id
- homework_assignment.publish_teacher_id -> sys_user.id
- homework_submission.assignment_id -> homework_assignment.id

## 常用口径
- 作业发布数 = COUNT(DISTINCT homework_assignment.id)，通常过滤 homework_assignment.deleted = 0
- 已发布答案作业数 = COUNT(DISTINCT homework_assignment.id)，过滤 answer_publish_status = PUBLISHED 且 deleted = 0

## 查询约束
- 查询 homework_assignment 必须带 LIMIT
- 大范围统计优先使用 class_id、course_detail_id、publish_teacher_id、status、homework_date 过滤
- 默认过滤 deleted = 0
