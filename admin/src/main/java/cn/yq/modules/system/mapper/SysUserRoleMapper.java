package cn.yq.modules.system.mapper;

import cn.yq.modules.system.domain.entity.SysUserRoleEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联Mapper。
 * 仅负责 sys_user_role 单表查询。
 */
public interface SysUserRoleMapper {

    List<SysUserRoleEntity> selectByUserId(@Param("userId") Long userId);
}
