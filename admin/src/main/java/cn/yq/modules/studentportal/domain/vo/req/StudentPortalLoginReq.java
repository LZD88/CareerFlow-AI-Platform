package cn.yq.modules.studentportal.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentPortalLoginReq {

    @NotBlank(message = "学号不能为空")
    private String studentCode;

    @NotBlank(message = "密码不能为空")
    private String password;
}
