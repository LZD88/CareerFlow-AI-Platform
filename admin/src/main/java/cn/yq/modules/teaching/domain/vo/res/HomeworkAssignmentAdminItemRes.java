package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

import java.util.Date;

@Data
public class HomeworkAssignmentAdminItemRes {

    private Long id;

    private String title;

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

    private String attachmentDownloadUrl;

    private String attachmentType;

    private String answerAttachmentName;

    private String answerAttachmentUrl;

    private String answerAttachmentDownloadUrl;

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

    private String description;
}
