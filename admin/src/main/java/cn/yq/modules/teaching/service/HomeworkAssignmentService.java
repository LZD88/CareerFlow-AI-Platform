package cn.yq.modules.teaching.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.teaching.domain.vo.req.HomeworkAnswerManageReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkAssignmentAdminQueryReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkAssignmentSaveReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkAssignmentStatusUpdateReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkSubmissionAdminQueryReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkSubmissionReviewReq;
import cn.yq.modules.teaching.domain.vo.res.HomeworkAssignmentAdminItemRes;
import cn.yq.modules.teaching.domain.vo.res.HomeworkPublishContextRes;
import cn.yq.modules.teaching.domain.vo.res.HomeworkSubmissionAdminItemRes;

import java.util.Date;

public interface HomeworkAssignmentService {

    PageResult<HomeworkAssignmentAdminItemRes> page(HomeworkAssignmentAdminQueryReq request);

    HomeworkAssignmentAdminItemRes detail(Long id);

    HomeworkPublishContextRes publishContext(Long classId, Date homeworkDate);

    Long create(HomeworkAssignmentSaveReq request, Long publishTeacherId);

    void update(Long id, HomeworkAssignmentSaveReq request);

    void updateStatus(Long id, HomeworkAssignmentStatusUpdateReq request, Long operatorId);

    void manageAnswer(Long id, HomeworkAnswerManageReq request, Long operatorId);

    PageResult<HomeworkSubmissionAdminItemRes> submissionPage(Long id, HomeworkSubmissionAdminQueryReq request);

    void reviewSubmission(Long id, Long submissionId, HomeworkSubmissionReviewReq request, Long operatorId);

    void delete(Long id);
}
