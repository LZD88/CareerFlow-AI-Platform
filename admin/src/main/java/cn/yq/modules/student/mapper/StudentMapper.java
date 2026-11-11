package cn.yq.modules.student.mapper;

import cn.yq.modules.student.domain.entity.StudentEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StudentMapper {

    int insert(StudentEntity entity);

    int updateById(StudentEntity entity);

    StudentEntity selectById(@Param("id") Long id);

    StudentEntity selectByStudentCode(@Param("studentCode") String studentCode);

    StudentEntity selectByPhone(@Param("phone") String phone);

    List<StudentEntity> selectPage(@Param("keyword") String keyword,
                                   @Param("studyMode") String studyMode,
                                   @Param("classId") Long classId,
                                   @Param("productId") Long productId);

    int deleteById(@Param("id") Long id);
}
