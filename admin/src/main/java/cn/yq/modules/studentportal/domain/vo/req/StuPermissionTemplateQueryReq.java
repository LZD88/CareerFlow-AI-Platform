package cn.yq.modules.studentportal.domain.vo.req;

import lombok.Data;

@Data
public class StuPermissionTemplateQueryReq {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;

    private Integer status;

    private String studyMode;

    private Long productId;

    private Long classId;
}
