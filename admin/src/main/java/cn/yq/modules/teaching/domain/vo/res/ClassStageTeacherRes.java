package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

import java.util.List;

@Data
public class ClassStageTeacherRes {

    private String stageName;

    private Integer dayCount;

    private String startDate;

    private String endDate;

    private Long teacherId;

    private String teacherName;

    private Boolean splitAssigned;

    private String teacherSummary;

    private List<StageTeacherOptionRes> availableTeachers;
}
