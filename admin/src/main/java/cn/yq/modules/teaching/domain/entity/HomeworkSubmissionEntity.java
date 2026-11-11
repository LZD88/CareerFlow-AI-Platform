package cn.yq.modules.teaching.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class HomeworkSubmissionEntity {

    private Long id;

    private Long assignmentId;

    private String studentNo;

    private String studentNameSnapshot;

    private String submitContent;

    private String attachmentName;

    private String attachmentUrl;

    private String attachmentType;

    private Date submitTime;

    private String status;

    private BigDecimal score;

    private String teacherComment;

    private Long reviewTeacherId;

    private Date reviewTime;

    private Integer version;

    private Date createTime;

    private Date updateTime;
}
