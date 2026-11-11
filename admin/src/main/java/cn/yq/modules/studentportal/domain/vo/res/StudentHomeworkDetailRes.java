package cn.yq.modules.studentportal.domain.vo.res;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class StudentHomeworkDetailRes {

    private Long id;

    private String title;

    private String description;

    private Long classId;

    private Long courseId;

    private Long courseDetailId;

    private String courseName;

    private Integer courseDayNum;

    private String courseContentSnapshot;

    private Date homeworkDate;

    private Date publishDate;

    private Date deadline;

    private String status;

    private Boolean allowResubmit;

    private String attachmentName;

    private String attachmentUrl;

    private String attachmentDownloadUrl;

    private String attachmentType;

    private String answerPublishStatus;

    private Date answerPublishTime;

    private String answerAttachmentName;

    private String answerAttachmentUrl;

    private String answerAttachmentDownloadUrl;

    private String answerAttachmentType;

    private String submissionStatus;

    private String submitContent;

    private String submissionAttachmentName;

    private String submissionAttachmentUrl;

    private String submissionAttachmentDownloadUrl;

    private String submissionAttachmentType;

    private Date submitTime;

    private BigDecimal score;

    private String teacherComment;

    private Integer version;
}
