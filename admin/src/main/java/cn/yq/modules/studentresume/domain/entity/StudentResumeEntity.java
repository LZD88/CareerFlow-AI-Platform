package cn.yq.modules.studentresume.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class StudentResumeEntity {

    private Long id;

    private Long studentId;

    private String studentCode;

    private String studentName;

    private String resumeName;

    private String targetRole;

    private String contentType;

    private String resumeText;

    private String resumeSummary;

    private String resumeFileUrl;

    private String parseStatus;

    private Boolean defaultResume;

    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
