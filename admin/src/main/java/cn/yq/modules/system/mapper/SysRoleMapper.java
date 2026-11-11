package cn.yq.modules.system.mapper;

import cn.yq.modules.system.domain.entity.SysPermissionEntity;
import cn.yq.modules.system.domain.entity.SysRoleEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysRoleMapper {

    int insert(SysRoleEntity role);

    int updateById(SysRoleEntity role);

    SysRoleEntity selectById(@Param("id") Long id);

    SysRoleEntity selectByCode(@Param("roleCode") String roleCode);

    List<SysRoleEntity> selectPage(@Param("keyword") String keyword, @Param("status") Integer status);

    int deleteById(@Param("id") Long id);

    List<SysPermissionEntity> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    int deleteRolePermissions(@Param("roleId") Long roleId);

    int insertRolePermissions(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);
}
