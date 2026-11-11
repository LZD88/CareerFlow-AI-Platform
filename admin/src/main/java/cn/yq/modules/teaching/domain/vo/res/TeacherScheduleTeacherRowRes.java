package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

import java.util.List;

@Data
public class TeacherScheduleTeacherRowRes {

    private Long teacherId;

    private String teacherName;

    private List<TeacherScheduleBlockRes> blocks;
}
