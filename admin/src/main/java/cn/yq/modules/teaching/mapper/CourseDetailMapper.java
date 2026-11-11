package cn.yq.modules.teaching.mapper;

import cn.yq.modules.teaching.domain.entity.CourseDetailEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseDetailMapper {

    int insert(CourseDetailEntity detail);

    int insertBatch(@Param("details") List<CourseDetailEntity> details);

    int updateById(CourseDetailEntity detail);

    int updateBatchById(@Param("details") List<CourseDetailEntity> details);

    int increaseDayNumFrom(@Param("courseId") Long courseId, @Param("dayNum") Integer dayNum);

    int decreaseDayNumAfter(@Param("courseId") Long courseId, @Param("dayNum") Integer dayNum);

    CourseDetailEntity selectById(@Param("id") Long id);

    List<CourseDetailEntity> selectByCourseId(@Param("courseId") Long courseId);

    int deleteById(@Param("id") Long id);

    int deleteByCourseId(@Param("courseId") Long courseId);
}
