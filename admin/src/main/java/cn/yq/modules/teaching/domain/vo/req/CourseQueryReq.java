package cn.yq.modules.teaching.domain.vo.req;

import lombok.Data;

@Data
public class CourseQueryReq {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;

    private Integer status;
}
