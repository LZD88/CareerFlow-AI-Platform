package cn.yq.modules.studentportal.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.modules.studentportal.biz.StudentPortalDailyMomentBiz;
import cn.yq.modules.studentportal.biz.StudentPortalProfileBiz;
import cn.yq.modules.studentportal.domain.vo.req.StudentDailyMomentCreateReq;
import cn.yq.modules.studentportal.domain.vo.req.StudentDailyMomentUploadPresignReq;
import cn.yq.modules.studentportal.domain.vo.res.StudentDailyMomentRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentProfileRes;
import cn.yq.modules.upload.domains.vo.res.UploadPresignRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生端个人资料入口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/student-api")
public class StudentPortalProfileController {

    private final StudentPortalProfileBiz studentPortalProfileBiz;
    private final StudentPortalDailyMomentBiz studentPortalDailyMomentBiz;

    /**
     * 查询当前登录学员的基础信息。
     */
    @GetMapping("/profile")
    public ApiResponse<StudentProfileRes> profile() {
        return ApiResponse.success(studentPortalProfileBiz.profile());
    }

    /**
     * 查询当前登录学员的日常剪影列表。
     */
    @GetMapping("/daily-moments")
    public ApiResponse<List<StudentDailyMomentRes>> dailyMoments() {
        return ApiResponse.success(studentPortalDailyMomentBiz.list());
    }

    /**
     * 生成学生日常剪影素材直传预签名地址。
     */
    @PostMapping("/daily-moments/upload-presign")
    public ApiResponse<UploadPresignRes> createDailyMomentUploadPresign(@RequestBody @Valid StudentDailyMomentUploadPresignReq request) {
        return ApiResponse.success(studentPortalDailyMomentBiz.createUploadPresign(request));
    }

    /**
     * 新增学生日常剪影。
     */
    @PostMapping("/daily-moments")
    public ApiResponse<Long> createDailyMoment(@RequestBody @Valid StudentDailyMomentCreateReq request) {
        return ApiResponse.success("创建成功", studentPortalDailyMomentBiz.create(request));
    }
}
