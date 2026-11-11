package cn.yq.modules.studentportal.biz;

import cn.yq.modules.studentportal.domain.vo.req.StudentHomeworkSubmitReq;
import cn.yq.modules.studentportal.domain.vo.req.StudentHomeworkUploadPresignReq;
import cn.yq.modules.studentportal.domain.vo.res.StudentHomeworkDetailRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentHomeworkItemRes;
import cn.yq.modules.upload.domains.vo.res.UploadPresignRes;

import java.util.List;

public interface StudentPortalHomeworkBiz {

    List<StudentHomeworkItemRes> list();

    StudentHomeworkDetailRes detail(Long assignmentId);

    UploadPresignRes createUploadPresign(StudentHomeworkUploadPresignReq request);

    Long submit(Long assignmentId, StudentHomeworkSubmitReq request);
}
