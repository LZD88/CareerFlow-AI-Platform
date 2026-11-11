package cn.yq.modules.system.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SysConfigUpdateReq {

    @NotBlank(message = "配置Key不能为空")
    private String k;

    @NotBlank(message = "配置Value不能为空")
    private String v;
}
