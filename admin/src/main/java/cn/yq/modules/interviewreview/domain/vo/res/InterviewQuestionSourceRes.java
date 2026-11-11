package cn.yq.modules.interviewreview.domain.vo.res;

import lombok.Data;

import java.util.Date;

@Data
public class InterviewQuestionSourceRes {

    private Long id;

    private Long questionId;

    private Long taskId;

    private String taskTitle;

    private String interviewRole;

    private String originalFilename;

    private String originalQuestion;

    private String answerContext;

    private String studentAnswerQuality;

    private Integer studentAnswerScore;

    private String studentAnswerAnalysis;

    private String improvementSuggestion;

    private Date createdAt;
}
