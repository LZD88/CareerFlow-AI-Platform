package cn.yq.modules.teaching.mapper;

import cn.yq.modules.teaching.domain.entity.HomeworkAssignmentEntity;
import cn.yq.modules.studentportal.domain.vo.res.StudentHomeworkDetailRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentHomeworkItemRes;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface HomeworkAssignmentMapper {

    int insert(HomeworkAssignmentEntity entity);

    int updateById(HomeworkAssignmentEntity entity);

    HomeworkAssignmentEntity selectById(@Param("id") Long id);

    List<HomeworkAssignmentEntity> selectAdminPage(@Param("title") String title,
                                                   @Param("classId") Long classId,
                                                   @Param("status") String status);

    List<StudentHomeworkItemRes> selectStudentList(@Param("classId") Long classId,
                                                   @Param("studentNo") String studentNo);

    StudentHomeworkDetailRes selectStudentDetail(@Param("id") Long id,
                                                 @Param("studentNo") String studentNo);

    int softDeleteById(@Param("id") Long id);
}
