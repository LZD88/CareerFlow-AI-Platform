package cn.yq.modules.interviewreview.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewAudioConfirmReq;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewAudioPresignReq;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewResumeConfirmReq;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewReviewTaskQueryReq;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewReviewTaskRes;
import cn.yq.modules.upload.domains.vo.res.UploadPresignRes;
import org.springframework.web.multipart.MultipartFile;

public interface InterviewReviewTaskService {

    PageResult<InterviewReviewTaskRes> page(InterviewReviewTaskQueryReq request);

    InterviewReviewTaskRes detail(Long id);

    InterviewReviewTaskRes uploadAudio(MultipartFile file,
                                       String title,
                                       String candidateName,
                                       String interviewRole,
                                       Long loginUserId);

    UploadPresignRes createAudioPresign(InterviewAudioPresignReq request);

    InterviewReviewTaskRes confirmAudioUpload(InterviewAudioConfirmReq request, Long loginUserId);

    InterviewReviewTaskRes confirmResumeUpload(Long id, InterviewResumeConfirmReq request);

    InterviewReviewTaskRes transcribe(Long id, Boolean speakerDiarization);

    InterviewReviewTaskRes generateReview(Long id);
}
