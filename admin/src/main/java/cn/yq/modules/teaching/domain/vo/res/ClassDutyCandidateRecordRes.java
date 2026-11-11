package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

import java.util.Date;

@Data
public class ClassDutyCandidateRecordRes {

    private Long classId;

    private Integer classTerm;

    private String campusName;

    private String courseName;

    private Date startTime;

    private Date graduateDate;

    private Long teacherId;

    private String teacherName;

    private String remark;
}
