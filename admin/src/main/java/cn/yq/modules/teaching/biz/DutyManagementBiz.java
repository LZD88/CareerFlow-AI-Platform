package cn.yq.modules.teaching.biz;

import cn.yq.modules.teaching.domain.vo.req.ClassDutySaveReq;
import cn.yq.modules.teaching.domain.vo.req.GlobalDutySaveReq;
import cn.yq.modules.teaching.domain.vo.res.DutyManagementDayRes;

public interface DutyManagementBiz {

    DutyManagementDayRes dayView(String dutyDate);

    void saveClassDuty(Long classId, ClassDutySaveReq request);

    void saveGlobalDuty(GlobalDutySaveReq request);
}
