package cn.yq.modules.teaching.service.impl;

import cn.yq.modules.teaching.domain.entity.ClassScheduleEntity;
import cn.yq.modules.teaching.domain.vo.res.TeacherScheduleRecordRes;
import cn.yq.modules.teaching.mapper.ClassScheduleMapper;
import cn.yq.modules.teaching.service.ClassScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassScheduleServiceImpl implements ClassScheduleService {

    private final ClassScheduleMapper classScheduleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBatch(List<ClassScheduleEntity> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        classScheduleMapper.insertBatch(records);
    }

    @Override
    public List<ClassScheduleEntity> listByClassId(Long classId) {
        return classScheduleMapper.selectByClassId(classId);
    }

    @Override
    public List<ClassScheduleEntity> listByClassIds(List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        return classScheduleMapper.selectByClassIds(classIds);
    }

    @Override
    public List<ClassScheduleEntity> listAssignedTeacherSchedules() {
        return classScheduleMapper.selectAssignedTeacherSchedules();
    }

    @Override
    public List<TeacherScheduleRecordRes> listTeacherSchedulesByDateRange(Date startDate, Date endDate, List<Long> teacherIds) {
        if (teacherIds == null || teacherIds.isEmpty()) {
            return List.of();
        }
        return classScheduleMapper.selectTeacherSchedulesByDateRange(startDate, endDate, teacherIds);
    }

    @Override
    public List<ClassScheduleEntity> listByClassIdAndDateRange(Long classId, Date startDate, Date endDate) {
        return classScheduleMapper.selectByClassIdAndDateRange(classId, startDate, endDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceFromDate(Long classId, Date startDate, List<ClassScheduleEntity> records) {
        classScheduleMapper.deleteByClassIdAndDateFrom(classId, startDate);
        if (records == null || records.isEmpty()) {
            return;
        }
        classScheduleMapper.insertBatch(records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTeacherByClassAndStage(Long classId, String stageName, Long teacherId) {
        classScheduleMapper.updateTeacherByClassAndStage(classId, stageName, teacherId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContentByClassAndDate(Long classId, Date scheduleDate, String courseContent) {
        classScheduleMapper.updateContentByClassAndDate(classId, scheduleDate, courseContent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTeacherByClassAndDate(Long classId, Date scheduleDate, Long teacherId) {
        classScheduleMapper.updateTeacherByClassAndDate(classId, scheduleDate, teacherId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByClassId(Long classId) {
        classScheduleMapper.deleteByClassId(classId);
    }

    @Override
    public boolean hasSchedule(Long classId) {
        return classScheduleMapper.countByClassId(classId) > 0;
    }
}
