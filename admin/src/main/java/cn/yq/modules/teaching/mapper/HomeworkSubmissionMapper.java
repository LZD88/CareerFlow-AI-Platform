package cn.yq.modules.teaching.mapper;

import cn.yq.modules.teaching.domain.entity.HomeworkSubmissionEntity;
import cn.yq.modules.teaching.domain.vo.res.HomeworkSubmissionAdminItemRes;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface HomeworkSubmissionMapper {

    HomeworkSubmissionEntity selectByAssignmentIdAndStudentNo(@Param("assignmentId") Long assignmentId,
                                                              @Param("studentNo") String studentNo);

    HomeworkSubmissionEntity selectById(@Param("id") Long id);

    List<HomeworkSubmissionAdminItemRes> selectAdminPage(@Param("assignmentId") Long assignmentId,
                                                         @Param("studentCode") String studentCode,
                                                         @Param("studentName") String studentName,
                                                         @Param("status") String status);

    int insert(HomeworkSubmissionEntity entity);

    int updateById(HomeworkSubmissionEntity entity);
}
