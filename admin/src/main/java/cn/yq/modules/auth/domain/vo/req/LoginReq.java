package cn.yq.modules.auth.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数。
 */
@Data
public class LoginReq {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
