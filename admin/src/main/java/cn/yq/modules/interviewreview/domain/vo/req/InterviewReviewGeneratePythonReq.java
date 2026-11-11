package cn.yq.modules.interviewreview.domain.vo.req;

import lombok.Data;

@Data
public class InterviewReviewGeneratePythonReq {

    private Long taskId;

    private String title;

    private String candidateName;

    private String interviewRole;

    private String transcriptText;

    private String resumeFilename;

    private String resumeObjectKey;

    private String resumeUrl;

    private String resumeContentType;
}
