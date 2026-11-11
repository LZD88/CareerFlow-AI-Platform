package cn.yq.modules.system.mapper;

import cn.yq.modules.system.domain.entity.SysPermissionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysPermissionMapper {

    int insert(SysPermissionEntity permission);

    int updateById(SysPermissionEntity permission);

    SysPermissionEntity selectById(@Param("id") Long id);

    SysPermissionEntity selectByCode(@Param("permissionCode") String permissionCode);

    List<SysPermissionEntity> selectPage(@Param("keyword") String keyword,
                                         @Param("permissionType") String permissionType,
                                         @Param("status") Integer status);

    List<SysPermissionEntity> selectAll();

    List<SysPermissionEntity> selectAllByIds(@Param("ids") List<Long> ids);

    int countByParentId(@Param("parentId") Long parentId);

    int deleteById(@Param("id") Long id);
}
