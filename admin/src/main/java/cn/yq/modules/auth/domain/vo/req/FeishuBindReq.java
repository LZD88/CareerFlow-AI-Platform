package cn.yq.modules.auth.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeishuBindReq {

    @NotBlank(message = "飞书授权码不能为空")
    private String code;
}
