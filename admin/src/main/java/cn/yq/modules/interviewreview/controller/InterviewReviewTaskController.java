package cn.yq.modules.interviewreview.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewAudioConfirmReq;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewAudioPresignReq;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewResumeConfirmReq;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewReviewTaskQueryReq;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewReviewTaskRes;
import cn.yq.modules.interviewreview.service.InterviewReviewTaskService;
import cn.yq.modules.upload.domains.vo.res.UploadPresignRes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interview-reviews")
public class InterviewReviewTaskController {

    private final InterviewReviewTaskService interviewReviewTaskService;

    @GetMapping
    public ApiResponse<PageResult<InterviewReviewTaskRes>> page(InterviewReviewTaskQueryReq request) {
        return ApiResponse.success(interviewReviewTaskService.page(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<InterviewReviewTaskRes> detail(@PathVariable Long id) {
        return ApiResponse.success(interviewReviewTaskService.detail(id));
    }

    @PostMapping("/audio")
    public ApiResponse<InterviewReviewTaskRes> uploadAudio(@RequestParam("file") MultipartFile file,
                                                           @RequestParam(value = "title", required = false) String title,
                                                           @RequestParam(value = "candidateName", required = false) String candidateName,
                                                           @RequestParam(value = "interviewRole", required = false) String interviewRole,
                                                           HttpServletRequest servletRequest) {
        return ApiResponse.success("上传成功", interviewReviewTaskService.uploadAudio(file, title, candidateName, interviewRole, getLoginUserId(servletRequest)));
    }

    @PostMapping("/audio/presign")
    public ApiResponse<UploadPresignRes> createAudioPresign(@RequestBody @Valid InterviewAudioPresignReq request) {
        return ApiResponse.success(interviewReviewTaskService.createAudioPresign(request));
    }

    @PostMapping("/audio/confirm")
    public ApiResponse<InterviewReviewTaskRes> confirmAudioUpload(@RequestBody @Valid InterviewAudioConfirmReq request,
                                                                  HttpServletRequest servletRequest) {
        return ApiResponse.success("上传成功", interviewReviewTaskService.confirmAudioUpload(request, getLoginUserId(servletRequest)));
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<InterviewReviewTaskRes> confirmResumeUpload(@PathVariable Long id,
                                                                   @RequestBody @Valid InterviewResumeConfirmReq request) {
        return ApiResponse.success("简历上传成功", interviewReviewTaskService.confirmResumeUpload(id, request));
    }

    @PostMapping("/{id}/transcribe")
    public ApiResponse<InterviewReviewTaskRes> transcribe(@PathVariable Long id,
                                                          @RequestParam(value = "speakerDiarization", required = false) Boolean speakerDiarization) {
        return ApiResponse.success("已开始转写", interviewReviewTaskService.transcribe(id, speakerDiarization));
    }

    @PostMapping("/{id}/review")
    public ApiResponse<InterviewReviewTaskRes> generateReview(@PathVariable Long id) {
        return ApiResponse.success("已开始生成复盘", interviewReviewTaskService.generateReview(id));
    }

    private Long getLoginUserId(HttpServletRequest request) {
        Object value = request.getAttribute("loginUserId");
        if (value == null) {
            throw new BusinessException(401, "登录状态已失效");
        }
        return Long.parseLong(value.toString());
    }
}
