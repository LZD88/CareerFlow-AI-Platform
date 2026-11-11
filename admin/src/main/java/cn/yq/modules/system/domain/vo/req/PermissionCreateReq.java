package cn.yq.modules.system.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PermissionCreateReq {

    @NotNull(message = "父权限不能为空")
    private Long parentId;

    @NotBlank(message = "权限编码不能为空")
    private String permissionCode;

    @NotBlank(message = "权限名称不能为空")
    private String permissionName;

    @NotBlank(message = "权限类型不能为空")
    private String permissionType;

    @NotNull(message = "排序值不能为空")
    private Integer sortNum;

    private String description;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
