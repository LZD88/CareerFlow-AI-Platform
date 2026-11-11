package cn.yq.modules.student.domain.vo.res;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class StudentDailyMomentAdminItemRes {

    private Long id;

    private String studentCode;

    private String studentName;

    private Long classId;

    private Integer classTerm;

    private String campusName;

    private String title;

    private String contentText;

    private List<String> fileUrls;

    private Date recordTime;

    private Date createTime;
}
