package cn.yq.modules.system.domain.vo.req;

import lombok.Data;

@Data
public class RoleQueryReq {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
    private Integer status;
}
