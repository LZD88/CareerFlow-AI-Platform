package cn.yq.modules.system.domain.vo.res;

import cn.yq.modules.system.domain.entity.SysPermissionEntity;
import cn.yq.modules.system.domain.entity.SysRoleEntity;
import lombok.Data;

import java.util.List;

@Data
public class RoleDetailRes {

    /** 角色基础信息 */
    private SysRoleEntity role;
    /** 角色绑定的权限列表 */
    private List<SysPermissionEntity> permissions;
}
