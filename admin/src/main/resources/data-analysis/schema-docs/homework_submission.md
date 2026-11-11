# homework_submission

业务含义：作业提交表，存储学员当前最新一次作业提交、提交状态、得分、老师评语、批改老师和批改时间。常用于分析作业提交率、批改情况和平均分。

## 字段
- id: 提交ID，主键
- assignment_id: 作业ID，关联 homework_assignment.id
- student_no: 学员编号，关联 edu_student.student_code
- student_name_snapshot: 学员姓名快照
- submit_time: 提交时间
- status: 提交状态，SUBMITTED已提交，REVIEWED已批改，RESUBMIT_REQUIRED需重交
- score: 得分
- review_teacher_id: 批改老师ID，关联 sys_user.id
- review_time: 批改时间
- version: 提交版本号
- create_time: 创建时间
- update_time: 更新时间

## 禁止查询字段
- submit_content: 提交内容，可能较长，一期不作为默认分析字段
- attachment_url: 提交附件地址，一期不作为默认分析字段
- teacher_comment: 老师评语，一期不作为默认分析字段

## 索引
- PRIMARY(id)
- uk_assignment_student(assignment_id, student_no)
- idx_student_no(student_no)
- idx_status(status)
- idx_review_teacher_id(review_teacher_id)

## 关联
- homework_submission.assignment_id -> homework_assignment.id
- homework_submission.student_no -> edu_student.student_code
- homework_submission.review_teacher_id -> sys_user.id

## 常用口径
- 作业提交数 = COUNT(DISTINCT homework_submission.id)
- 已批改数 = COUNT(DISTINCT homework_submission.id)，过滤 homework_submission.status = REVIEWED
- 平均作业得分 = AVG(homework_submission.score)，过滤 homework_submission.status = REVIEWED 且 score IS NOT NULL

## 查询约束
- 查询 homework_submission 必须带 LIMIT
- 大范围统计优先使用 assignment_id、student_no、status、review_teacher_id 过滤
- 不允许展示 submit_content、teacher_comment、attachment_url
