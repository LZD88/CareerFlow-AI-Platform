package cn.yq.modules.system.domain.vo.req;

import lombok.Data;

@Data
public class UserQueryReq {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
    private Integer status;
    private String roleCode;
}
