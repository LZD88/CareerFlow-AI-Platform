package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

@Data
public class TeacherScheduleBlockRes {

    private String startDate;

    private String endDate;

    private Integer startIndex;

    private Integer spanDays;

    private Long classId;

    private Integer classTerm;

    private String classLabel;

    private String stageName;
}
