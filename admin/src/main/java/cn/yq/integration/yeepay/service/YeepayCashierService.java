package cn.yq.integration.yeepay.service;

import cn.yq.integration.yeepay.domain.YeepayUnifiedOrderReq;
import cn.yq.integration.yeepay.domain.YeepayUnifiedOrderRes;

public interface YeepayCashierService {

    YeepayUnifiedOrderRes unifiedOrder(YeepayUnifiedOrderReq request);
}
