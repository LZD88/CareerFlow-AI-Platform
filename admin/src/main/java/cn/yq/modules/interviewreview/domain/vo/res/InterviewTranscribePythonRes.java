package cn.yq.modules.interviewreview.domain.vo.res;

import lombok.Data;

@Data
public class InterviewTranscribePythonRes {

    private Long taskId;

    private String audioObjectKey;

    private String text;
}
