package cn.yq.modules.studentportal.mapper;

import cn.yq.modules.studentportal.domain.entity.StuPermissionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StuPermissionMapper {

    List<StuPermissionEntity> selectAll();

    List<StuPermissionEntity> selectAllByIds(@Param("ids") List<Long> ids);
}
