package cn.yq.modules.sales.domain.vo.req;

import lombok.Data;

@Data
public class OrderQueryReq {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;

    private String orderStatus;

    private String fulfillmentStatus;
}
