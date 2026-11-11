package cn.yq.modules.studentportal.biz;

import cn.yq.modules.studentportal.domain.vo.req.StudentPortalLoginReq;
import cn.yq.modules.studentportal.domain.vo.res.StudentPortalLoginRes;

public interface StudentPortalAuthBiz {

    StudentPortalLoginRes login(StudentPortalLoginReq request);
}
