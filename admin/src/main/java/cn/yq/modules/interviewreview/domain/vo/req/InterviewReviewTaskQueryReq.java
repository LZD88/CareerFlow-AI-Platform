package cn.yq.modules.interviewreview.domain.vo.req;

import lombok.Data;

@Data
public class InterviewReviewTaskQueryReq {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;

    private String status;
}
