package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

@Data
public class ClassScheduleCalendarDayRes {

    /** 日期 yyyy-MM-dd */
    private String date;

    /** 当月中的日 */
    private Integer dayOfMonth;

    /** 是否当前查询月份 */
    private Boolean currentMonth;

    /** 课表标签：上课 / 自习 / 休息 */
    private String label;

    /** 课表类型：CLASS / SELF_STUDY / REST */
    private String scheduleType;

    /** 节假日名称 */
    private String holidayName;

    /** 是否节假日 */
    private Boolean holiday;

    /** 课程第几天 */
    private Integer courseDayNum;

    /** 课程内容 */
    private String courseContent;

    /** 所属阶段 */
    private String stageName;

    /** 授课老师ID */
    private Long teacherId;

    /** 授课老师名称 */
    private String teacherName;
}
