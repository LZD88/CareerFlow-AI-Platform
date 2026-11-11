package cn.yq.modules.studentportal.mapper;

import cn.yq.modules.studentportal.domain.entity.StuPermissionTemplateEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StuPermissionTemplateMapper {

    StuPermissionTemplateEntity selectById(@Param("id") Long id);

    StuPermissionTemplateEntity selectByCode(@Param("templateCode") String templateCode);

    List<StuPermissionTemplateEntity> selectPage(@Param("keyword") String keyword,
                                                 @Param("status") Integer status,
                                                 @Param("studyMode") String studyMode,
                                                 @Param("productId") Long productId,
                                                 @Param("classId") Long classId);

    List<StuPermissionTemplateEntity> selectAllActive();

    int insert(StuPermissionTemplateEntity entity);

    int updateById(StuPermissionTemplateEntity entity);

    int deleteById(@Param("id") Long id);
}
