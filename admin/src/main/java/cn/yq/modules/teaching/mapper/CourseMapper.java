package cn.yq.modules.teaching.mapper;

import cn.yq.modules.teaching.domain.entity.CourseEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseMapper {

    int insert(CourseEntity course);

    int updateById(CourseEntity course);

    CourseEntity selectById(@Param("id") Long id);

    List<CourseEntity> selectPage(@Param("keyword") String keyword, @Param("status") Integer status);

    int deleteById(@Param("id") Long id);
}
