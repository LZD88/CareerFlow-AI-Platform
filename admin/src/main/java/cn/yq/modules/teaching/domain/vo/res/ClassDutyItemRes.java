package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

@Data
public class ClassDutyItemRes {

    private Long classId;

    private String classLabel;

    private String campusName;

    private String courseName;

    private String startTime;

    private String graduateDate;

    private String dutyLabel;

    private String dutyTimeRange;

    private Long teacherId;

    private String teacherName;

    private String remark;
}
