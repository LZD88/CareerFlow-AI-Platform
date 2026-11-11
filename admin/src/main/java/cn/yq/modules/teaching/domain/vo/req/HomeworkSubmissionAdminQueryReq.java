package cn.yq.modules.teaching.domain.vo.req;

import lombok.Data;

@Data
public class HomeworkSubmissionAdminQueryReq {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String studentCode;

    private String studentName;

    private String status;
}
