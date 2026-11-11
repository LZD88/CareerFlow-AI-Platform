package cn.yq.modules.interviewreview.mapper;

import cn.yq.modules.interviewreview.domain.entity.InterviewQuestionBankEntity;
import cn.yq.modules.interviewreview.domain.entity.InterviewQuestionSourceEntity;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewQuestionSourceRes;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InterviewQuestionBankMapper {

    List<InterviewQuestionBankEntity> selectPage(@Param("keyword") String keyword,
                                                 @Param("category") String category);

    InterviewQuestionBankEntity selectById(@Param("id") Long id);

    InterviewQuestionBankEntity selectByNormalizedQuestion(@Param("normalizedQuestion") String normalizedQuestion);

    int insertQuestion(InterviewQuestionBankEntity entity);

    int updateQuestion(InterviewQuestionBankEntity entity);

    int incrementSourceCount(@Param("id") Long id,
                             @Param("lastSourceTaskId") Long lastSourceTaskId);

    int insertSource(InterviewQuestionSourceEntity entity);

    List<InterviewQuestionSourceRes> selectSourcesByQuestionId(@Param("questionId") Long questionId);
}
