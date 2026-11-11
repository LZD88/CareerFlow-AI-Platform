package cn.yq.modules.teaching.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class HomeworkAssignmentEntity {

    private Long id;

    private String title;

    private String description;

    private Long classId;

    private Integer classTerm;

    private String campusName;

    private Long courseId;

    private String courseName;

    private Long courseDetailId;

    private Integer courseDayNum;

    private String courseContentSnapshot;

    private Date homeworkDate;

    private String scheduleType;

    private String attachmentName;

    private String attachmentUrl;

    private String attachmentType;

    private String answerAttachmentName;

    private String answerAttachmentUrl;

    private String answerAttachmentType;

    private String answerPublishStatus;

    private Date answerPublishTime;

    private Long answerPublishTeacherId;

    private String answerPublishTeacherName;

    private Long publishTeacherId;

    private String publishTeacherName;

    private Date publishDate;

    private Date deadline;

    private String status;

    private Boolean allowResubmit;

    private Integer studentCountSnapshot;

    private Integer submittedCount;

    private Boolean deleted;

    private Date closeTime;

    private Long closeTeacherId;

    private String closeTeacherName;

    private Date createTime;

    private Date updateTime;
}
