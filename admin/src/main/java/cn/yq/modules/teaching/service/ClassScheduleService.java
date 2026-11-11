package cn.yq.modules.teaching.service;

import cn.yq.modules.teaching.domain.entity.ClassScheduleEntity;
import cn.yq.modules.teaching.domain.vo.res.TeacherScheduleRecordRes;

import java.util.Date;
import java.util.List;

public interface ClassScheduleService {

    void createBatch(List<ClassScheduleEntity> records);

    List<ClassScheduleEntity> listByClassId(Long classId);

    List<ClassScheduleEntity> listByClassIds(List<Long> classIds);

    List<ClassScheduleEntity> listAssignedTeacherSchedules();

    List<TeacherScheduleRecordRes> listTeacherSchedulesByDateRange(Date startDate, Date endDate, List<Long> teacherIds);

    List<ClassScheduleEntity> listByClassIdAndDateRange(Long classId, Date startDate, Date endDate);

    void replaceFromDate(Long classId, Date startDate, List<ClassScheduleEntity> records);

    void updateTeacherByClassAndStage(Long classId, String stageName, Long teacherId);

    void updateContentByClassAndDate(Long classId, Date scheduleDate, String courseContent);

    void updateTeacherByClassAndDate(Long classId, Date scheduleDate, Long teacherId);

    void deleteByClassId(Long classId);

    boolean hasSchedule(Long classId);
}
