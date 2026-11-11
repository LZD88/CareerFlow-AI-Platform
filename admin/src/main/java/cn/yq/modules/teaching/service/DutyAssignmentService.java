package cn.yq.modules.teaching.service;

import cn.yq.modules.teaching.domain.entity.DutyAssignmentEntity;
import cn.yq.modules.teaching.domain.vo.res.ClassDutyCandidateRecordRes;

import java.util.Date;
import java.util.List;

public interface DutyAssignmentService {

    List<ClassDutyCandidateRecordRes> listClassDutyCandidates(Date dutyDate);

    DutyAssignmentEntity getGlobalDuty(Date dutyDate, String dutyType);

    void save(DutyAssignmentEntity entity);
}
