package cn.yq.integration.yeepay.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class YeepayUnifiedOrderRes {

    String uniqueOrderNo;

    String cashierUrl;
}
