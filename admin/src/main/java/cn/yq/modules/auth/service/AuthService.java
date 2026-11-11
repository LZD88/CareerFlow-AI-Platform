package cn.yq.modules.auth.service;

import cn.yq.modules.auth.domain.vo.req.LoginReq;
import cn.yq.modules.auth.domain.vo.req.FeishuBindReq;
import cn.yq.modules.auth.domain.vo.req.ChangePasswordReq;
import cn.yq.modules.auth.domain.vo.res.AuthProfileRes;
import cn.yq.modules.auth.domain.vo.res.LoginRes;

/**
 * 登录认证服务接口。
 */
public interface AuthService {

    LoginRes login(LoginReq request);

    LoginRes feishuLogin(String code);

    AuthProfileRes profile(Long loginUserId);

    void bindFeishu(Long loginUserId, FeishuBindReq request);

    void unbindFeishu(Long loginUserId);

    void changePassword(Long loginUserId, ChangePasswordReq request);
}
