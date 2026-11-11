package cn.yq.modules.studentportal.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.modules.studentportal.biz.StudentPortalAuthBiz;
import cn.yq.modules.studentportal.domain.vo.req.StudentPortalLoginReq;
import cn.yq.modules.studentportal.domain.vo.res.StudentPortalLoginRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生端认证入口。
 *
 * <p>学生端不走后台权限体系，但除登录接口外，其余接口都要求学生端 JWT + 请求签名。</p>
 * <p>登录后会根据学员档案和订单状态分三类流转：
 * 1. 已有学员档案：直接登录成功；
 * 2. 存在待支付订单：进入支付页；
 * 3. 存在已支付但未建档订单：进入入学资料填写页。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/student-api")
public class StudentPortalAuthController {

    private final StudentPortalAuthBiz studentPortalAuthBiz;

    /**
     * 学生端登录。
     */
    @PostMapping("/auth/login")
    public ApiResponse<StudentPortalLoginRes> login(@RequestBody @Valid StudentPortalLoginReq request) {
        return ApiResponse.success("登录成功", studentPortalAuthBiz.login(request));
    }
}
