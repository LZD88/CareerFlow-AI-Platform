package cn.yq.modules.system.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleUpdateReq {

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private String description;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
