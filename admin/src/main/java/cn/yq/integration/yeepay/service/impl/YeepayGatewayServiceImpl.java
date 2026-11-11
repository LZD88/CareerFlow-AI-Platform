package cn.yq.integration.yeepay.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.yq.common.exception.BusinessException;
import cn.yq.config.YeepayDynamicProperties;
import cn.yq.integration.yeepay.domain.YeepayApiRequest;
import cn.yq.integration.yeepay.domain.YeepayApiResponse;
import cn.yq.integration.yeepay.service.YeepayGatewayService;
import cn.yq.integration.yeepay.service.YeepayReadiness;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.yeepay.yop.sdk.exception.YopClientBizException;
import com.yeepay.yop.sdk.exception.YopClientException;
import com.yeepay.yop.sdk.service.common.YopClient;
import com.yeepay.yop.sdk.service.common.YopClientBuilder;
import com.yeepay.yop.sdk.service.common.request.YopRequest;
import com.yeepay.yop.sdk.service.common.response.YopResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class YeepayGatewayServiceImpl implements YeepayGatewayService {

    private static final String YEEPAY_PROVIDER = "yeepay";
    private static final String SANDBOX_ENV = "sandbox";

    private final YeepayDynamicProperties yeepayDynamicProperties;
    private final YeepayReadiness readiness;

    /**
     * 注意易宝 SDK client 需要单例复用，避免重复 build。
     */
    private volatile YopClient prodClient;
    private volatile YopClient sandboxClient;

    @Override
    public YeepayApiResponse execute(YeepayApiRequest request) {
        readiness.requireReady();
        ensureEnabled();
        YopRequest yopRequest = new YopRequest(request.getApiUri(), request.getHttpMethod());
        if (request.getParameters() != null) {
            request.getParameters().forEach((key, value) -> {
                if (value != null) {
                    yopRequest.addParameter(key, value);
                }
            });
        }

        try {
            YopResponse response = currentClient().request(yopRequest);
            String rawResult = StrUtil.blankToDefault(response.getStringResult(), JSON.toJSONString(response.getResult()));
            JSONObject result = toJsonObject(response.getResult(), rawResult);
            return YeepayApiResponse.builder()
                    .requestId(response.getMetadata() == null ? null : response.getMetadata().getYopRequestId())
                    .rawResult(rawResult)
                    .result(result)
                    .build();
        } catch (YopClientBizException ex) {
            log.error("Yeepay biz error, apiUri={}, errorCode={}, message={}", request.getApiUri(), ex.getErrorCode(), ex.getMessage(), ex);
            throw new BusinessException(400, "易宝接口调用失败: " + ex.getMessage());
        } catch (YopClientException ex) {
            log.error("Yeepay client error, apiUri={}, message={}", request.getApiUri(), ex.getMessage(), ex);
            throw new BusinessException(500, "易宝客户端调用异常，请稍后重试");
        } catch (Exception ex) {
            log.error("Yeepay unknown error, apiUri={}", request.getApiUri(), ex);
            throw new BusinessException(500, "支付下单失败，请稍后重试");
        }
    }

    private synchronized YopClient currentClient() {
        if (Boolean.TRUE.equals(yeepayDynamicProperties.getUseSandbox())) {
            if (sandboxClient == null) sandboxClient = YopClientBuilder.builder().withProvider(YEEPAY_PROVIDER).withEnv(SANDBOX_ENV).build();
            return sandboxClient;
        }
        if (prodClient == null) prodClient = YopClientBuilder.builder().build();
        return prodClient;
    }

    private void ensureEnabled() {
        if (!Boolean.TRUE.equals(yeepayDynamicProperties.getEnabled())) {
            throw new BusinessException(400, "易宝支付未启用");
        }
    }

    private JSONObject toJsonObject(Object result, String rawResult) {
        if (result instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        if (result instanceof Map<?, ?> map) {
            return new JSONObject(map);
        }
        if (StrUtil.isBlank(rawResult)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(rawResult);
        } catch (Exception ex) {
            JSONObject fallback = new JSONObject();
            fallback.put("value", rawResult);
            return fallback;
        }
    }
}
