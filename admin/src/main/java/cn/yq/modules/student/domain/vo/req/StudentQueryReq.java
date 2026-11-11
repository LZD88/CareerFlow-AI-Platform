package cn.yq.modules.student.domain.vo.req;

import lombok.Data;

@Data
public class StudentQueryReq {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;

    private String studyMode;

    private Long classId;

    private Long productId;
}
