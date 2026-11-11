package cn.yq.modules.interviewreview.mapper;

import cn.yq.modules.interviewreview.domain.entity.InterviewReviewTaskEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InterviewReviewTaskMapper {

    int insert(InterviewReviewTaskEntity entity);

    InterviewReviewTaskEntity selectById(@Param("id") Long id);

    List<InterviewReviewTaskEntity> selectPage(@Param("keyword") String keyword,
                                               @Param("status") String status);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("failReason") String failReason);

    int updateTranscribing(@Param("id") Long id);

    int updateTranscribed(@Param("id") Long id,
                          @Param("transcriptText") String transcriptText);

    int updateReviewing(@Param("id") Long id);

    int updateReviewed(@Param("id") Long id,
                       @Param("reviewReport") String reviewReport);

    int updateResume(@Param("id") Long id,
                     @Param("resumeFilename") String resumeFilename,
                     @Param("resumeObjectKey") String resumeObjectKey,
                     @Param("resumeUrl") String resumeUrl,
                     @Param("resumeContentType") String resumeContentType,
                     @Param("resumeSize") Long resumeSize);

    int updateQuestionGenerating(@Param("id") Long id);

    int updateQuestionGenerated(@Param("id") Long id,
                                @Param("extractedCount") Integer extractedCount,
                                @Param("createdCount") Integer createdCount,
                                @Param("mergedCount") Integer mergedCount);

    int updateQuestionFailed(@Param("id") Long id,
                             @Param("failReason") String failReason);
}
