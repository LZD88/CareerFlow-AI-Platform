package cn.yq.modules.teaching.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.teaching.domain.vo.req.HomeworkAnswerManageReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkAssignmentAdminQueryReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkAssignmentSaveReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkAssignmentStatusUpdateReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkSubmissionAdminQueryReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkSubmissionReviewReq;
import cn.yq.modules.teaching.domain.vo.res.HomeworkAssignmentAdminItemRes;
import cn.yq.modules.teaching.domain.vo.res.HomeworkPublishContextRes;
import cn.yq.modules.teaching.domain.vo.res.HomeworkSubmissionAdminItemRes;
import cn.yq.modules.teaching.service.HomeworkAssignmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teaching/homeworks")
public class HomeworkAssignmentController {

    private final HomeworkAssignmentService homeworkAssignmentService;

    @GetMapping
    public ApiResponse<PageResult<HomeworkAssignmentAdminItemRes>> page(HomeworkAssignmentAdminQueryReq request) {
        return ApiResponse.success(homeworkAssignmentService.page(request));
    }

    @GetMapping("/publish-context")
    public ApiResponse<HomeworkPublishContextRes> publishContext(@RequestParam Long classId,
                                                                 @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date homeworkDate) {
        return ApiResponse.success(homeworkAssignmentService.publishContext(classId, homeworkDate));
    }

    @GetMapping("/{id}")
    public ApiResponse<HomeworkAssignmentAdminItemRes> detail(@PathVariable Long id) {
        return ApiResponse.success(homeworkAssignmentService.detail(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid HomeworkAssignmentSaveReq request,
                                    HttpServletRequest servletRequest) {
        return ApiResponse.success("发布成功", homeworkAssignmentService.create(request, getLoginUserId(servletRequest)));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid HomeworkAssignmentSaveReq request) {
        homeworkAssignmentService.update(id, request);
        return ApiResponse.success("更新成功", null);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @RequestBody @Valid HomeworkAssignmentStatusUpdateReq request,
                                          HttpServletRequest servletRequest) {
        homeworkAssignmentService.updateStatus(id, request, getLoginUserId(servletRequest));
        return ApiResponse.success("状态更新成功", null);
    }

    @PutMapping("/{id}/answer")
    public ApiResponse<Void> manageAnswer(@PathVariable Long id,
                                          @RequestBody @Valid HomeworkAnswerManageReq request,
                                          HttpServletRequest servletRequest) {
        homeworkAssignmentService.manageAnswer(id, request, getLoginUserId(servletRequest));
        return ApiResponse.success("答案保存成功", null);
    }

    @GetMapping("/{id}/submissions")
    public ApiResponse<PageResult<HomeworkSubmissionAdminItemRes>> submissionPage(@PathVariable Long id,
                                                                                  HomeworkSubmissionAdminQueryReq request) {
        return ApiResponse.success(homeworkAssignmentService.submissionPage(id, request));
    }

    @PutMapping("/{id}/submissions/{submissionId}/review")
    public ApiResponse<Void> reviewSubmission(@PathVariable Long id,
                                              @PathVariable Long submissionId,
                                              @RequestBody @Valid HomeworkSubmissionReviewReq request,
                                              HttpServletRequest servletRequest) {
        homeworkAssignmentService.reviewSubmission(id, submissionId, request, getLoginUserId(servletRequest));
        return ApiResponse.success("批改成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        homeworkAssignmentService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    private Long getLoginUserId(HttpServletRequest request) {
        Object value = request.getAttribute("loginUserId");
        if (value == null) {
            throw new BusinessException(401, "登录状态已失效");
        }
        return Long.parseLong(value.toString());
    }
}
