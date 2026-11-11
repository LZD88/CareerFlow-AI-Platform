-- 允许班级课表写入自习 / 休息等非正式课程记录
-- 适用于已存在 edu_class_schedule 表的存量环境

ALTER TABLE edu_class_schedule
    MODIFY COLUMN course_detail_id BIGINT NULL COMMENT '课程详情ID，正式上课日才有值',
    MODIFY COLUMN course_day_num INT NULL COMMENT '课程第几天，正式上课日才有值',
    MODIFY COLUMN schedule_type VARCHAR(32) NOT NULL DEFAULT 'CLASS' COMMENT '排课类型 CLASS/SELF_STUDY/REST',
    MODIFY COLUMN course_content VARCHAR(2000) NULL COMMENT '课程内容或当天安排说明',
    MODIFY COLUMN stage_name VARCHAR(100) NULL COMMENT '课程所属阶段，自习/休息可为空';
