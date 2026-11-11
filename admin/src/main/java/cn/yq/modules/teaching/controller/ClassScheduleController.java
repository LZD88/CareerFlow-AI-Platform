package cn.yq.modules.teaching.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.modules.teaching.biz.ClassScheduleBiz;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课表管理控制器。
 * 这里聚合了班级课表的生成、单天调整、老师分配以及月历查询等接口，
 * 前端课表管理页面只需要围绕这个控制器完成大部分交互。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teaching/schedules")
public class ClassScheduleController {

    private final ClassScheduleBiz classScheduleBiz;

    /**
     * 为指定班级生成课表。
     * 前端会先让运营选择第一天上课日期，后端再结合课程详情和节假日信息落表。
     */
    @PostMapping("/classes/{classId}/generate")
    public ApiResponse<Void> generate(@PathVariable Long classId, @RequestBody @Valid ClassScheduleGenerateReq request) {
        classScheduleBiz.generate(classId, request);
        return ApiResponse.success("生成成功", null);
    }

    @PostMapping("/classes/{classId}/arrange")
    public ApiResponse<Void> arrange(@PathVariable Long classId, @RequestBody @Valid ClassScheduleArrangeReq request) {
        classScheduleBiz.arrange(classId, request);
        return ApiResponse.success("保存成功", null);
    }

    /**
     * 查询“新增课程 / 变更老师”时某一天可选的授课老师。
     * 规则是：老师当天在其他班级不能有课，但当前班当天已有课不算冲突。
     */
    @GetMapping("/classes/{classId}/arrange")
    public ApiResponse<List<StageTeacherOptionRes>> availableArrangeTeachers(@PathVariable Long classId,
                                                                             @RequestParam String scheduleDate) {
        return ApiResponse.success(classScheduleBiz.availableArrangeTeachers(classId, scheduleDate));
    }

    /**
     * 单独修改当天课表记录的课程内容。
     * 这里只改课表快照，不回写课程模板。
     */
    @PutMapping("/classes/{classId}/arrange/content")
    public ApiResponse<Void> changeContent(@PathVariable Long classId, @RequestBody @Valid ClassScheduleChangeContentReq request) {
        classScheduleBiz.changeContent(classId, request);
        return ApiResponse.success("变更成功", null);
    }

    /**
     * 单独修改当天课表记录的授课老师。
     * 保存前会再次校验老师当天是否在其他班级有课。
     */
    @PutMapping("/classes/{classId}/arrange/teacher")
    public ApiResponse<Void> changeTeacher(@PathVariable Long classId, @RequestBody @Valid ClassScheduleChangeTeacherReq request) {
        classScheduleBiz.changeTeacher(classId, request);
        return ApiResponse.success("变更成功", null);
    }

    /**
     * 删除单天课表。
     * 对正式上课日会触发后续课程前移；对固定休息 / 自习日则只恢复当天固定状态。
     */
    @DeleteMapping("/classes/{classId}/arrange")
    public ApiResponse<Void> deleteSchedule(@PathVariable Long classId, @RequestBody @Valid ClassScheduleDeleteReq request) {
        classScheduleBiz.deleteSchedule(classId, request);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 查询班级各阶段的老师分配视图。
     * 如果阶段内部已经存在单天换老师，会返回“已拆分授课”状态供前端展示。
     */
    @GetMapping("/classes/{classId}/stage-teachers")
    public ApiResponse<List<ClassStageTeacherRes>> stageTeachers(@PathVariable Long classId) {
        return ApiResponse.success(classScheduleBiz.stageTeachers(classId));
    }

    /**
     * 按阶段批量回写老师到每日课表记录。
     * 这是“统一分配老师”的入口，不会覆盖后续用户再次做的单天变更。
     */
    @PutMapping("/classes/{classId}/stage-teachers")
    public ApiResponse<Void> assignStageTeachers(@PathVariable Long classId, @RequestBody @Valid AssignStageTeacherReq request) {
        classScheduleBiz.assignStageTeachers(classId, request);
        return ApiResponse.success("分配成功", null);
    }

    /**
     * 查询班级某个月的月历课表视图。
     * 返回结果已经包含上课 / 自习 / 休息等展示态，前端直接渲染月视图即可。
     */
    @GetMapping("/classes/{classId}/calendar")
    public ApiResponse<ClassScheduleCalendarRes> calendar(@PathVariable Long classId,
                                                          @RequestParam(required = false) Integer year,
                                                          @RequestParam(required = false) Integer month) {
        return ApiResponse.success(classScheduleBiz.calendar(classId, year, month));
    }

    /**
     * 查询老师维度的课表视图。
     * 支持按时间范围、老师姓名关键字和指定老师筛选，
     * 返回结果同时包含有课老师行与无课老师列表，前端可直接渲染横向课表。
     */
    @GetMapping("/teachers/calendar")
    public ApiResponse<TeacherScheduleCalendarRes> teacherCalendar(@RequestParam(required = false) String startDate,
                                                                   @RequestParam(required = false) String endDate,
                                                                   @RequestParam(required = false) String keyword,
                                                                   @RequestParam(required = false) Long teacherId) {
        return ApiResponse.success(classScheduleBiz.teacherCalendar(startDate, endDate, keyword, teacherId));
    }
}
