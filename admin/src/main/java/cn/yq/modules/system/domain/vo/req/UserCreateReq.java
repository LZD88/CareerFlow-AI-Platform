package cn.yq.modules.system.domain.vo.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserCreateReq {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String realName;
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
