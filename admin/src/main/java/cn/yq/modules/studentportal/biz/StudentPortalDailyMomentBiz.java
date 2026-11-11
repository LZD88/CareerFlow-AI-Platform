package cn.yq.modules.studentportal.biz;

import cn.yq.modules.studentportal.domain.vo.req.StudentDailyMomentCreateReq;
import cn.yq.modules.studentportal.domain.vo.req.StudentDailyMomentUploadPresignReq;
import cn.yq.modules.studentportal.domain.vo.res.StudentDailyMomentRes;
import cn.yq.modules.upload.domains.vo.res.UploadPresignRes;

import java.util.List;

public interface StudentPortalDailyMomentBiz {

    List<StudentDailyMomentRes> list();

    UploadPresignRes createUploadPresign(StudentDailyMomentUploadPresignReq request);

    Long create(StudentDailyMomentCreateReq request);
}
