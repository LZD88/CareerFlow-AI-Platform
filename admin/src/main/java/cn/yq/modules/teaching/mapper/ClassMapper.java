package cn.yq.modules.teaching.mapper;

import cn.yq.modules.teaching.domain.entity.ClassEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ClassMapper {

    int insert(ClassEntity entity);

    int updateById(ClassEntity entity);

    ClassEntity selectById(@Param("id") Long id);

    List<ClassEntity> selectPage(@Param("keyword") String keyword,
                                 @Param("classStatus") String classStatus,
                                 @Param("campusId") Long campusId,
                                 @Param("headTeacherId") Long headTeacherId,
                                 @Param("courseId") Long courseId);

    int deleteById(@Param("id") Long id);
}
