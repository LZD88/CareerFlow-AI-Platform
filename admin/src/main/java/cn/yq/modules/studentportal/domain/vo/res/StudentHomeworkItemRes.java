package cn.yq.modules.studentportal.domain.vo.res;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class StudentHomeworkItemRes {

    private Long id;

    private String title;

    private String description;

    private String courseName;

    private String courseContentSnapshot;

    private Date homeworkDate;

    private Date publishDate;

    private Date deadline;

    private String status;

    private Boolean allowResubmit;

    private String attachmentName;

    private String attachmentDownloadUrl;

    private String answerPublishStatus;

    private String answerAttachmentName;

    private String answerAttachmentDownloadUrl;

    private String submissionStatus;

    private String submitContent;

    private String submissionAttachmentName;

    private String submissionAttachmentDownloadUrl;

    private Date submitTime;

    private BigDecimal score;

    private String teacherComment;

    private Integer version;
}
