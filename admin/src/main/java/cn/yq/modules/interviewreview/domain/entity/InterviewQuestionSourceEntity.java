package cn.yq.modules.interviewreview.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class InterviewQuestionSourceEntity {

    private Long id;

    private Long questionId;

    private Long taskId;

    private String originalQuestion;

    private String answerContext;

    private String studentAnswerQuality;

    private Integer studentAnswerScore;

    private String studentAnswerAnalysis;

    private String improvementSuggestion;

    private Date createdAt;
}
