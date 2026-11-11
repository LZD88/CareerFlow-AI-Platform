package cn.yq.modules.teaching.domain.entity;

import lombok.Data;

import java.util.Date;

/**
 * 班级课表实体。
 * 一条记录对应班级某一天的安排，可能是上课 / 自习 / 休息。
 */
@Data
public class ClassScheduleEntity {

    /** 课表主键 ID */
    private Long id;

    /** 班级 ID */
    private Long classId;

    /** 课程 ID */
    private Long courseId;

    /** 课程详情 ID，非正式课程日可为空 */
    private Long courseDetailId;

    /** 课程第几天，非正式课程日可为空 */
    private Integer courseDayNum;

    /** 排课日期 */
    private Date scheduleDate;

    /** 排课类型：CLASS / SELF_STUDY / REST */
    private String scheduleType;

    /** 课程内容或当天安排说明 */
    private String courseContent;

    /** 所属阶段 */
    private String stageName;

    /** 授课老师ID */
    private Long teacherId;

    /** 授课老师姓名 */
    private String teacherName;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;
}
