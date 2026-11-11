package cn.yq.modules.studentportal.biz;

import cn.yq.modules.studentportal.domain.vo.req.mockinterview.MockInterviewCreateSessionReq;
import cn.yq.modules.studentportal.domain.vo.res.mockinterview.MockInterviewSessionRes;

public interface StudentPortalMockInterviewBiz {

    MockInterviewSessionRes createSession(MockInterviewCreateSessionReq request);
}
