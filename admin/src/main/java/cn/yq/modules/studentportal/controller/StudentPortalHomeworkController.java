package cn.yq.modules.studentportal.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.modules.studentportal.biz.StudentPortalHomeworkBiz;
import cn.yq.modules.studentportal.domain.vo.req.StudentHomeworkSubmitReq;
import cn.yq.modules.studentportal.domain.vo.req.StudentHomeworkUploadPresignReq;
import cn.yq.modules.studentportal.domain.vo.res.StudentHomeworkDetailRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentHomeworkItemRes;
import cn.yq.modules.upload.domains.vo.res.UploadPresignRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student-api/homeworks")
public class StudentPortalHomeworkController {

    private final StudentPortalHomeworkBiz studentPortalHomeworkBiz;

    @GetMapping
    public ApiResponse<List<StudentHomeworkItemRes>> list() {
        return ApiResponse.success(studentPortalHomeworkBiz.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentHomeworkDetailRes> detail(@PathVariable Long id) {
        return ApiResponse.success(studentPortalHomeworkBiz.detail(id));
    }

    @PostMapping("/upload-presign")
    public ApiResponse<UploadPresignRes> createUploadPresign(@RequestBody @Valid StudentHomeworkUploadPresignReq request) {
        return ApiResponse.success(studentPortalHomeworkBiz.createUploadPresign(request));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<Long> submit(@PathVariable Long id, @RequestBody @Valid StudentHomeworkSubmitReq request) {
        return ApiResponse.success("提交成功", studentPortalHomeworkBiz.submit(id, request));
    }
}
