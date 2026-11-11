package cn.yq.modules.teaching.biz;

import cn.yq.modules.teaching.domain.vo.req.AssignStageTeacherReq;
import cn.yq.modules.teaching.domain.vo.req.ClassScheduleArrangeReq;
import cn.yq.modules.teaching.domain.vo.req.ClassScheduleChangeContentReq;
import cn.yq.modules.teaching.domain.vo.req.ClassScheduleChangeTeacherReq;
import cn.yq.modules.teaching.domain.vo.req.ClassScheduleDeleteReq;
import cn.yq.modules.teaching.domain.vo.req.ClassScheduleGenerateReq;
import cn.yq.modules.teaching.domain.vo.res.ClassStageTeacherRes;
import cn.yq.modules.teaching.domain.vo.res.ClassScheduleCalendarRes;
import cn.yq.modules.teaching.domain.vo.res.StageTeacherOptionRes;
import cn.yq.modules.teaching.domain.vo.res.TeacherScheduleCalendarRes;

import java.util.List;

/**
 * 课表业务编排接口。
 * 这一层负责把课表生成、单天调整、老师分配、月历聚合等跨 service 的业务规则串起来，
 * controller 只做参数接收与响应封装。
 */
public interface ClassScheduleBiz {

    /** 按首课日期与课程模板生成完整班级课表。 */
    void generate(Long classId, ClassScheduleGenerateReq request);

    /** 单天调整课表，可新增上课 / 自习 / 休息，并按规则决定是否顺延后续课程。 */
    void arrange(Long classId, ClassScheduleArrangeReq request);

    /** 查询某一天可选授课老师，供新增课程或单天变更老师时使用。 */
    List<StageTeacherOptionRes> availableArrangeTeachers(Long classId, String scheduleDate);

    /** 只修改当天课表快照中的课程内容，不影响课程模板。 */
    void changeContent(Long classId, ClassScheduleChangeContentReq request);

    /** 只修改当天课表快照中的授课老师。 */
    void changeTeacher(Long classId, ClassScheduleChangeTeacherReq request);

    /** 删除单天课表，并根据当天类型决定是否前移后续课程。 */
    void deleteSchedule(Long classId, ClassScheduleDeleteReq request);

    /** 查询班级各阶段老师分配情况。 */
    List<ClassStageTeacherRes> stageTeachers(Long classId);

    /** 按阶段批量分配老师到每日课表记录。 */
    void assignStageTeachers(Long classId, AssignStageTeacherReq request);

    /** 查询班级月历视图。 */
    ClassScheduleCalendarRes calendar(Long classId, Integer year, Integer month);

    /** 查询老师课表视图。 */
    TeacherScheduleCalendarRes teacherCalendar(String startDate, String endDate, String keyword, Long teacherId);
}
