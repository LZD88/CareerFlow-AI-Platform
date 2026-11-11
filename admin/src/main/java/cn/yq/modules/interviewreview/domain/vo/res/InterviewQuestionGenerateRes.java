package cn.yq.modules.interviewreview.domain.vo.res;

import lombok.Data;

@Data
public class InterviewQuestionGenerateRes {

    private String status;

    private Integer extractedCount;

    private Integer createdCount;

    private Integer mergedCount;
}
