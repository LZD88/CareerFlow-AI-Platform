package cn.yq.modules.interviewreview.domain.vo.res;

import lombok.Data;

import java.util.List;

@Data
public class InterviewQuestionExtractPythonRes {

    private Long taskId;

    private List<Question> questions;

    @Data
    public static class Question {

        private String originalQuestion;

        private String normalizedQuestion;

        private String category;

        private List<String> tags;

        private String answerContext;

        private String studentAnswerQuality;

        private Integer studentAnswerScore;

        private String studentAnswerAnalysis;

        private String improvementSuggestion;

        private String standardAnswer;

        private String shortAnswer;

        private List<String> followUpQuestions;

        private List<String> pitfalls;
    }
}
