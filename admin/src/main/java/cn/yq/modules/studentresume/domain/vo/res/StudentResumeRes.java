package cn.yq.modules.studentresume.domain.vo.res;

import lombok.Data;

import java.util.Date;

@Data
public class StudentResumeRes {

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

    private String resumeFilePreviewUrl;

    private String parseStatus;

    private Boolean defaultResume;

    private Date createdAt;

    private Date updatedAt;
}
