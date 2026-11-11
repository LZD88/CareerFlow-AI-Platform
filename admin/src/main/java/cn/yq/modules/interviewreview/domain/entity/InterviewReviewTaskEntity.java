package cn.yq.modules.interviewreview.domain.entity;

import lombok.Data;

import java.util.Date;

/**
 * 面试复盘任务。
 */
@Data
public class InterviewReviewTaskEntity {

    private Long id;

    private String title;

    private String candidateName;

    private String interviewRole;

    private String originalFilename;

    private String audioObjectKey;

    private String audioUrl;

    private String audioContentType;

    private Long audioSize;

    private String resumeFilename;

    private String resumeObjectKey;

    private String resumeUrl;

    private String resumeContentType;

    private Long resumeSize;

    private String status;

    private String transcriptText;

    private String reviewReport;

    private String failReason;

    private String questionStatus;

    private Integer questionExtractedCount;

    private Integer questionCreatedCount;

    private Integer questionMergedCount;

    private String questionFailReason;

    private Long createdBy;

    private Date createdAt;

    private Date updatedAt;
}
