package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

import java.util.List;

@Data
public class TeacherScheduleCalendarRes {

    private String startDate;

    private String endDate;

    private List<TeacherScheduleDateRes> dateHeaders;

    private List<TeacherScheduleTeacherRowRes> teachers;

    private List<TeacherScheduleIdleTeacherRes> idleTeachers;
}
