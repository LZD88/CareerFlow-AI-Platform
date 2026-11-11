package cn.yq.modules.interviewreview.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class InterviewQuestionBankEntity {

    private Long id;

    private String normalizedQuestion;

    private String category;

    private String tags;

    private String standardAnswer;

    private String shortAnswer;

    private String followUpQuestions;

    private String pitfalls;

    private Integer sourceCount;

    private Long lastSourceTaskId;

    private Date createdAt;

    private Date updatedAt;
}
