package cn.yq.modules.teaching.mapper;

import cn.yq.modules.teaching.domain.entity.ClassScheduleEntity;
import cn.yq.modules.teaching.domain.vo.res.TeacherScheduleRecordRes;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface ClassScheduleMapper {

    int insertBatch(@Param("records") List<ClassScheduleEntity> records);

    List<ClassScheduleEntity> selectByClassId(@Param("classId") Long classId);

    List<ClassScheduleEntity> selectByClassIdAndDateRange(@Param("classId") Long classId,
                                                          @Param("startDate") Date startDate,
                                                          @Param("endDate") Date endDate);

    List<ClassScheduleEntity> selectByClassIds(@Param("classIds") List<Long> classIds);

    List<ClassScheduleEntity> selectAssignedTeacherSchedules();

    List<TeacherScheduleRecordRes> selectTeacherSchedulesByDateRange(@Param("startDate") Date startDate,
                                                                     @Param("endDate") Date endDate,
                                                                     @Param("teacherIds") List<Long> teacherIds);

    int deleteByClassIdAndDateFrom(@Param("classId") Long classId, @Param("startDate") Date startDate);

    int updateTeacherByClassAndStage(@Param("classId") Long classId,
                                     @Param("stageName") String stageName,
                                     @Param("teacherId") Long teacherId);

    int updateContentByClassAndDate(@Param("classId") Long classId,
                                    @Param("scheduleDate") Date scheduleDate,
                                    @Param("courseContent") String courseContent);

    int updateTeacherByClassAndDate(@Param("classId") Long classId,
                                    @Param("scheduleDate") Date scheduleDate,
                                    @Param("teacherId") Long teacherId);

    int deleteByClassId(@Param("classId") Long classId);

    int countByClassId(@Param("classId") Long classId);
}
