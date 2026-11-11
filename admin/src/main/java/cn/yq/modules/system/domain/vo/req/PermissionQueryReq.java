package cn.yq.modules.system.domain.vo.req;

import lombok.Data;

@Data
public class PermissionQueryReq {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String keyword;
    private String permissionType;
    private Integer status;
}
