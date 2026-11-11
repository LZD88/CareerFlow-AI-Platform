package cn.yq.modules.interviewreview.domain.vo.req;

import lombok.Data;

@Data
public class InterviewTranscribePythonReq {

    private Long taskId;

    private String audioObjectKey;

    private String audioUrl;

    private String language = "auto";

    private Boolean speakerDiarization = Boolean.FALSE;
}
