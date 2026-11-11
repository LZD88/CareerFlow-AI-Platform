package cn.yq.modules.teaching.service;

import cn.yq.modules.teaching.domain.entity.StudentDailyMomentEntity;

import java.util.List;

public interface StudentDailyMomentService {

    Long create(StudentDailyMomentEntity entity);

    List<StudentDailyMomentEntity> listByStudentNo(String studentNo);
}
