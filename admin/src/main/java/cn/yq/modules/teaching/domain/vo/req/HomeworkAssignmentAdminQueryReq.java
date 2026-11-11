package cn.yq.modules.teaching.domain.vo.req;

import lombok.Data;

@Data
public class HomeworkAssignmentAdminQueryReq {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String title;

    private Long classId;

    private String status;
}
