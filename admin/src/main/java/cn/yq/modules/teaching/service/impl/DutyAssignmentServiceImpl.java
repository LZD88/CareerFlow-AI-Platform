package cn.yq.modules.teaching.service.impl;

import cn.yq.modules.teaching.domain.entity.DutyAssignmentEntity;
import cn.yq.modules.teaching.domain.vo.res.ClassDutyCandidateRecordRes;
import cn.yq.modules.teaching.mapper.DutyAssignmentMapper;
import cn.yq.modules.teaching.service.DutyAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DutyAssignmentServiceImpl implements DutyAssignmentService {

    private final DutyAssignmentMapper dutyAssignmentMapper;

    @Override
    public List<ClassDutyCandidateRecordRes> listClassDutyCandidates(Date dutyDate) {
        return dutyAssignmentMapper.selectClassDutyCandidates(dutyDate);
    }

    @Override
    public DutyAssignmentEntity getGlobalDuty(Date dutyDate, String dutyType) {
        return dutyAssignmentMapper.selectGlobalDuty(dutyDate, dutyType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(DutyAssignmentEntity entity) {
        dutyAssignmentMapper.upsert(entity);
    }
}
