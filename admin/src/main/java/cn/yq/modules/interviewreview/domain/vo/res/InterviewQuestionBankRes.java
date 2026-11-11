package cn.yq.modules.interviewreview.domain.vo.res;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class InterviewQuestionBankRes {

    private Long id;

    private String normalizedQuestion;

    private String category;

    private List<String> tags;

    private String standardAnswer;

    private String shortAnswer;

    private List<String> followUpQuestions;

    private List<String> pitfalls;

    private Integer sourceCount;

    private Long lastSourceTaskId;

    private Date createdAt;

    private Date updatedAt;
}
