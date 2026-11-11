package cn.yq.modules.teaching.domain.vo.req;

import lombok.Data;

@Data
public class ClassQueryReq {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;

    private String classStatus;

    private Long campusId;

    private Long headTeacherId;

    private Long courseId;
}
