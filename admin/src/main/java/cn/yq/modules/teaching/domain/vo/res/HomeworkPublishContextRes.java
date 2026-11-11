package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

import java.util.Date;

@Data
public class HomeworkPublishContextRes {

    private Long classId;

    private Integer classTerm;

    private String campusName;

    private Long courseId;

    private String courseName;

    private Long courseDetailId;

    private Integer courseDayNum;

    private String courseContentSnapshot;

    private String scheduleType;

    private Date homeworkDate;
}
