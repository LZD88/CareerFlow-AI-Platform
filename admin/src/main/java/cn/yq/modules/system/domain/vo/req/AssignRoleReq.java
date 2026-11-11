package cn.yq.modules.system.domain.vo.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignRoleReq {

    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}
