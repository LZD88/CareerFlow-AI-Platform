package cn.yq.modules.system.mapper;

import cn.yq.modules.system.domain.entity.SysPermissionEntity;
import cn.yq.modules.system.domain.entity.SysRoleEntity;
import cn.yq.modules.system.domain.entity.SysUserEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserMapper {

    int insert(SysUserEntity user);

    int updateById(SysUserEntity user);

    SysUserEntity selectById(@Param("id") Long id);

    SysUserEntity selectByUsername(@Param("username") String username);

    SysUserEntity selectByUnionId(@Param("unionId") String unionId);

    List<SysUserEntity> selectPage(@Param("keyword") String keyword,
                                   @Param("status") Integer status,
                                   @Param("roleCode") String roleCode);

    int deleteById(@Param("id") Long id);

    int updateUnionId(@Param("id") Long id, @Param("unionId") String unionId);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    List<SysRoleEntity> selectRolesByUserId(@Param("userId") Long userId);

    List<SysPermissionEntity> selectRolePermissionsByUserId(@Param("userId") Long userId);

    int deleteUserRoles(@Param("userId") Long userId);

    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
