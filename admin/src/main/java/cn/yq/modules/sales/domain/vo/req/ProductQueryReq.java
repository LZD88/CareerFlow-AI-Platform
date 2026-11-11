package cn.yq.modules.sales.domain.vo.req;

import lombok.Data;

@Data
public class ProductQueryReq {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;
}
