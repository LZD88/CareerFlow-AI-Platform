package cn.yq.modules.student.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.student.domain.vo.req.StudentDailyMomentAdminQueryReq;
import cn.yq.modules.student.domain.vo.res.StudentDailyMomentAdminItemRes;

public interface StudentDailyMomentManageService {

    PageResult<StudentDailyMomentAdminItemRes> page(StudentDailyMomentAdminQueryReq request);
}
