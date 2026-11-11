package cn.yq.modules.teaching.mapper;

import cn.yq.modules.teaching.domain.entity.StudentDailyMomentEntity;
import cn.yq.modules.teaching.domain.entity.StudentDailyMomentAdminEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StudentDailyMomentMapper {

    int insert(StudentDailyMomentEntity entity);

    List<StudentDailyMomentEntity> selectByStudentNo(@Param("studentNo") String studentNo);

    List<StudentDailyMomentAdminEntity> selectAdminPage(@Param("studentCode") String studentCode,
                                                        @Param("studentName") String studentName,
                                                        @Param("title") String title);
}
