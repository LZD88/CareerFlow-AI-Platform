package cn.yq.modules.system.domain.vo.res;

import cn.yq.modules.system.domain.entity.SysPermissionEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PermissionTreeRes extends SysPermissionEntity {

    /** 子权限节点 */
    private List<PermissionTreeRes> children = new ArrayList<>();
}
