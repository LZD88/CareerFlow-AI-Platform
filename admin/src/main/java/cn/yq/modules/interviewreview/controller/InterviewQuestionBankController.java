package cn.yq.modules.interviewreview.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewQuestionQueryReq;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewQuestionBankRes;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewQuestionGenerateRes;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewQuestionSourceRes;
import cn.yq.modules.interviewreview.service.InterviewQuestionBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interview-questions")
public class InterviewQuestionBankController {

    private final InterviewQuestionBankService interviewQuestionBankService;

    @GetMapping
    public ApiResponse<PageResult<InterviewQuestionBankRes>> page(InterviewQuestionQueryReq request) {
        return ApiResponse.success(interviewQuestionBankService.page(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<InterviewQuestionBankRes> detail(@PathVariable Long id) {
        return ApiResponse.success(interviewQuestionBankService.detail(id));
    }

    @GetMapping("/{id}/sources")
    public ApiResponse<List<InterviewQuestionSourceRes>> sources(@PathVariable Long id) {
        return ApiResponse.success(interviewQuestionBankService.sources(id));
    }

    @PostMapping("/generate-from-task/{taskId}")
    public ApiResponse<InterviewQuestionGenerateRes> generateFromTask(@PathVariable Long taskId) {
        return ApiResponse.success("问题总结已生成", interviewQuestionBankService.generateFromTask(taskId));
    }
}
