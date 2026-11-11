package cn.yq.modules.interviewreview.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewQuestionQueryReq;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewQuestionBankRes;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewQuestionGenerateRes;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewQuestionSourceRes;

import java.util.List;

public interface InterviewQuestionBankService {

    PageResult<InterviewQuestionBankRes> page(InterviewQuestionQueryReq request);

    InterviewQuestionBankRes detail(Long id);

    List<InterviewQuestionSourceRes> sources(Long id);

    InterviewQuestionGenerateRes generateFromTask(Long taskId);
}
