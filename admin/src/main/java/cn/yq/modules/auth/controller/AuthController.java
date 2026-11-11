package cn.yq.modules.auth.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.auth.domain.vo.req.ChangePasswordReq;
import cn.yq.modules.auth.domain.vo.req.FeishuBindReq;
import cn.yq.modules.auth.domain.vo.req.LoginReq;
import cn.yq.modules.auth.domain.vo.res.AuthProfileRes;
import cn.yq.modules.auth.domain.vo.res.LoginRes;
import cn.yq.modules.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录认证控制器。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginRes> login(@RequestBody @Valid LoginReq request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @PostMapping("/feishu-login")
    public ApiResponse<LoginRes> feishuLogin(@RequestBody @Valid FeishuBindReq request) {
        return ApiResponse.success("飞书登录成功", authService.feishuLogin(request.getCode()));
    }

    @GetMapping("/profile")
    public ApiResponse<AuthProfileRes> profile(HttpServletRequest request) {
        return ApiResponse.success(authService.profile(getLoginUserId(request)));
    }

    @PostMapping("/feishu/bind")
    public ApiResponse<Void> bindFeishu(@RequestBody @Valid FeishuBindReq request, HttpServletRequest httpServletRequest) {
        authService.bindFeishu(getLoginUserId(httpServletRequest), request);
        return ApiResponse.success("飞书绑定成功", null);
    }

    @DeleteMapping("/feishu/bind")
    public ApiResponse<Void> unbindFeishu(HttpServletRequest request) {
        authService.unbindFeishu(getLoginUserId(request));
        return ApiResponse.success("飞书解绑成功", null);
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody @Valid ChangePasswordReq request, HttpServletRequest httpServletRequest) {
        authService.changePassword(getLoginUserId(httpServletRequest), request);
        return ApiResponse.success("密码修改成功", null);
    }

    private Long getLoginUserId(HttpServletRequest request) {
        Object value = request.getAttribute("loginUserId");
        if (value == null) {
            throw new BusinessException(401, "登录状态已失效");
        }
        return Long.parseLong(value.toString());
    }
}
