package cn.yq.integration.yeepay.service;

import cn.yq.integration.yeepay.domain.YeepayApiRequest;
import cn.yq.integration.yeepay.domain.YeepayApiResponse;

public interface YeepayGatewayService {

    YeepayApiResponse execute(YeepayApiRequest request);
}
