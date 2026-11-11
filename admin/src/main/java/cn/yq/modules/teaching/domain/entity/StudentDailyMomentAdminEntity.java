package cn.yq.modules.teaching.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class StudentDailyMomentAdminEntity {

    private Long id;

    private String studentCode;

    private String studentName;

    private Long classId;

    private Integer classTerm;

    private String campusName;

    private String title;

    private String contentText;

    private String fileUrl;

    private Date recordTime;

    private Date createTime;
}
