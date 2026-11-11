package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

import java.util.Date;

@Data
public class TeacherScheduleRecordRes {

    private Long teacherId;

    private String teacherName;

    private Date scheduleDate;

    private Long classId;

    private Integer classTerm;

    private String stageName;
}
