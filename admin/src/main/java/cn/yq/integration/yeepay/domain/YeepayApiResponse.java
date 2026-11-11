package cn.yq.integration.yeepay.domain;

import com.alibaba.fastjson2.JSONObject;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class YeepayApiResponse {

    String requestId;

    String rawResult;

    JSONObject result;
}
