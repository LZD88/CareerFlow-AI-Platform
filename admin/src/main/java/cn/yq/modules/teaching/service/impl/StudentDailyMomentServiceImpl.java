package cn.yq.modules.teaching.service.impl;

import cn.yq.modules.teaching.domain.entity.StudentDailyMomentEntity;
import cn.yq.modules.teaching.mapper.StudentDailyMomentMapper;
import cn.yq.modules.teaching.service.StudentDailyMomentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentDailyMomentServiceImpl implements StudentDailyMomentService {

    private final StudentDailyMomentMapper studentDailyMomentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StudentDailyMomentEntity entity) {
        Date now = new Date();
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(now);
        }
        entity.setUpdateTime(now);
        studentDailyMomentMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public List<StudentDailyMomentEntity> listByStudentNo(String studentNo) {
        return studentDailyMomentMapper.selectByStudentNo(studentNo);
    }
}
