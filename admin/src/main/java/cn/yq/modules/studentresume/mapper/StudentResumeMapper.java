package cn.yq.modules.studentresume.mapper;

import cn.yq.modules.studentresume.domain.entity.StudentResumeEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StudentResumeMapper {

    int insert(StudentResumeEntity entity);

    int update(StudentResumeEntity entity);

    StudentResumeEntity selectById(@Param("id") Long id);

    StudentResumeEntity selectByIdAndStudentId(@Param("id") Long id, @Param("studentId") Long studentId);

    List<StudentResumeEntity> selectByStudentId(@Param("studentId") Long studentId);

    List<StudentResumeEntity> selectPage(@Param("keyword") String keyword,
                                         @Param("targetRole") String targetRole,
                                         @Param("defaultResume") Boolean defaultResume);

    int clearDefault(@Param("studentId") Long studentId, @Param("excludeId") Long excludeId);

    int setDefault(@Param("id") Long id, @Param("studentId") Long studentId);

    int softDelete(@Param("id") Long id);

    int softDeleteByStudent(@Param("id") Long id, @Param("studentId") Long studentId);
}
