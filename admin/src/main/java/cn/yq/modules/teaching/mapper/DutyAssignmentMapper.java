package cn.yq.modules.teaching.mapper;

import cn.yq.modules.teaching.domain.entity.DutyAssignmentEntity;
import cn.yq.modules.teaching.domain.vo.res.ClassDutyCandidateRecordRes;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface DutyAssignmentMapper {

    List<ClassDutyCandidateRecordRes> selectClassDutyCandidates(@Param("dutyDate") Date dutyDate);

    DutyAssignmentEntity selectGlobalDuty(@Param("dutyDate") Date dutyDate, @Param("dutyType") String dutyType);

    int upsert(DutyAssignmentEntity entity);
}
