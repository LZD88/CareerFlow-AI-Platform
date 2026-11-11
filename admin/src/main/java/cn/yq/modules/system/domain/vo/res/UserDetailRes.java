package cn.yq.modules.system.domain.vo.res;

import cn.yq.modules.system.domain.entity.SysPermissionEntity;
import cn.yq.modules.system.domain.entity.SysRoleEntity;
import cn.yq.modules.system.domain.entity.SysUserEntity;
import lombok.Data;

import java.util.List;

@Data
public class UserDetailRes {

    /** 用户基础信息 */
    private SysUserEntity user;
    /** 用户绑定的角色 */
    private List<SysRoleEntity> roles;
    /** 通过角色聚合得到的权限 */
    private List<SysPermissionEntity> rolePermissions;
}
