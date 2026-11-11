package cn.yq.integration.yeepay.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class YeepayApiRequest {

    String apiUri;

    String httpMethod;

    Map<String, Object> parameters;
}
