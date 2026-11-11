package cn.yq.modules.studentportal.mapper;

import cn.yq.modules.studentportal.domain.entity.StuTemplatePermissionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StuTemplatePermissionMapper {

    List<StuTemplatePermissionEntity> selectByTemplateId(@Param("templateId") Long templateId);

    int deleteByTemplateId(@Param("templateId") Long templateId);

    int insertTemplatePermissions(@Param("templateId") Long templateId, @Param("permissionIds") List<Long> permissionIds);
}
