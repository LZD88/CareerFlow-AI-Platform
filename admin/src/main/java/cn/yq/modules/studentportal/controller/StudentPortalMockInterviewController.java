package cn.yq.modules.studentportal.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.modules.studentportal.biz.StudentPortalMockInterviewBiz;
import cn.yq.modules.studentportal.domain.vo.req.mockinterview.MockInterviewCreateSessionReq;
import cn.yq.modules.studentportal.domain.vo.res.mockinterview.MockInterviewSessionRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student-api/mock-interviews")
public class StudentPortalMockInterviewController {

    private final StudentPortalMockInterviewBiz studentPortalMockInterviewBiz;

    @PostMapping("/sessions")
    public ApiResponse<MockInterviewSessionRes> createSession(@RequestBody(required = false) @Valid MockInterviewCreateSessionReq request) {
        return ApiResponse.success("模拟面试会话创建成功", studentPortalMockInterviewBiz.createSession(request));
    }
}
