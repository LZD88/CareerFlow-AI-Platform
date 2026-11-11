package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class HomeworkSubmissionAdminItemRes {

    private Long submissionId;

    private Long studentId;

    private String studentCode;

    private String studentName;

    private String phone;

    private Integer graduationSession;

    private String school;

    private String status;

    private String submitContent;

    private String attachmentName;

    private String attachmentUrl;

    private String attachmentDownloadUrl;

    private String attachmentType;

    private Date submitTime;

    private BigDecimal score;

    private String teacherComment;

    private Long reviewTeacherId;

    private String reviewTeacherName;

    private Date reviewTime;

    private Integer version;
}
