package cn.yq.modules.studentportal.domain.vo.res;

import lombok.Data;

import java.util.Date;

@Data
public class StudentProfileRes {

    private Long studentId;

    private String studentCode;

    private String studentName;

    private String phone;

    private Integer graduationSession;

    private String school;

    private String education;

    private String studyMode;

    private Long classId;

    private String className;

    private Long productId;

    private String productName;

    private Date joinTime;
}
