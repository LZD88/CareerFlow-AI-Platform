package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

@Data
public class GlobalDutyItemRes {

    private String dutyLabel;

    private String dutyTimeRange;

    private Long teacherId;

    private String teacherName;

    private String remark;
}
