package cn.yq.modules.system.mapper;

import cn.yq.modules.system.domain.entity.SysRolePermissionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联Mapper。
 * 仅负责 sys_role_permission 单表查询。
 */
public interface SysRolePermissionMapper {

    List<SysRolePermissionEntity> selectByRoleIds(@Param("roleIds") List<Long> roleIds);
}
