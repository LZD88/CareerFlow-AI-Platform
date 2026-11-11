package cn.yq.modules.studentportal.mapper;

import cn.yq.modules.studentportal.domain.entity.StuStudentPermissionTemplateEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StuStudentPermissionTemplateMapper {

    List<StuStudentPermissionTemplateEntity> selectByStudentId(@Param("studentId") Long studentId);

    StuStudentPermissionTemplateEntity selectByStudentIdAndTemplateId(@Param("studentId") Long studentId,
                                                                      @Param("templateId") Long templateId);

    int insert(StuStudentPermissionTemplateEntity entity);

    int updateById(StuStudentPermissionTemplateEntity entity);

    int disableByStudentId(@Param("studentId") Long studentId);
}
