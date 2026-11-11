# edu_student_daily_moment

业务含义：学生日常剪影表，存储学生提交的图片、视频和文字素材记录。常用于统计素材提交数量、学生活跃情况和按时间分析提交趋势。

## 字段
- id: 主键ID
- student_no: 学员编号，关联 edu_student.student_code
- title: 素材标题
- record_time: 素材时间
- create_time: 创建时间
- update_time: 最后更新时间

## 禁止查询字段
- content_text: 文字内容，一期不作为默认分析字段
- file_url: 文件访问URL，敏感字段，禁止直接查询和展示

## 索引
- PRIMARY(id)
- idx_student_no(student_no)
- idx_record_time(record_time)

## 关联
- edu_student_daily_moment.student_no -> edu_student.student_code

## 常用口径
- 素材提交数 = COUNT(DISTINCT edu_student_daily_moment.id)
- 提交学员数 = COUNT(DISTINCT edu_student_daily_moment.student_no)

## 查询约束
- 查询 edu_student_daily_moment 必须带 LIMIT
- 大范围统计优先使用 student_no、record_time 过滤
- 不允许展示 file_url
