package cn.yq.modules.interviewreview.domain.vo.req;

import lombok.Data;

@Data
public class InterviewQuestionExtractPythonReq {

    private Long taskId;

    private String title;

    private String interviewRole;

    private String transcriptText;

    private String resumeText;

    private String resumeFilename;

    private String resumeUrl;

    private String resumeContentType;
}
