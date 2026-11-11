package cn.yq.integration.yeepay.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.yq.common.exception.BusinessException;
import cn.yq.config.YeepayDynamicProperties;
import cn.yq.config.YeepayProperties;
import cn.yq.integration.yeepay.domain.YeepayApiRequest;
import cn.yq.integration.yeepay.domain.YeepayApiResponse;
import cn.yq.integration.yeepay.domain.YeepayUnifiedOrderReq;
import cn.yq.integration.yeepay.domain.YeepayUnifiedOrderRes;
import cn.yq.integration.yeepay.service.YeepayCashierService;
import cn.yq.integration.yeepay.service.YeepayGatewayService;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class YeepayCashierServiceImpl implements YeepayCashierService {

    private static final String UNIFIED_ORDER_API_URI = "/rest/v1.0/cashier/unified/order";
    private static final String HTTP_METHOD_POST = "POST";
    private static final DateTimeFormatter EXPIRED_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final YeepayDynamicProperties yeepayDynamicProperties;
    private final YeepayProperties yeepayProperties;
    private final YeepayGatewayService yeepayGatewayService;

    @Override
    public YeepayUnifiedOrderRes unifiedOrder(YeepayUnifiedOrderReq request) {
        validateRequest(request);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("parentMerchantNo", resolveParentMerchantNo());
        params.put("merchantNo", yeepayProperties.getMerchantNo().trim());
        params.put("orderId", request.getPaymentOrderNo().trim());
        params.put("orderAmount", request.getOrderAmount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        params.put("goodsName", request.getGoodsName().trim());
        params.put("fundProcessType", yeepayProperties.getFundProcessType());
        params.put("notifyUrl", getNotifyUrl());
        params.put("memo", buildMemo(request));
        params.put("expiredTime", (request.getExpiresAt() == null ? LocalDateTime.now().plusMinutes(getExpireMinutes())
                : LocalDateTime.ofInstant(request.getExpiresAt().toInstant(), java.time.ZoneId.of("Asia/Shanghai"))).format(EXPIRED_TIME_FORMATTER));
        params.put("returnUrl", buildReturnUrl(request));
        params.put("limitPayType", trimToNull(yeepayDynamicProperties.getLimitPayType()));

        YeepayApiResponse response = yeepayGatewayService.execute(YeepayApiRequest.builder()
                .apiUri(UNIFIED_ORDER_API_URI)
                .httpMethod(HTTP_METHOD_POST)
                .parameters(params)
                .build());

        JSONObject result = response.getResult();
        String url = extractCashierUrl(result);
        if (StrUtil.isBlank(extractUniqueOrderNo(result)) || !isSafeCashierUrl(url))
            throw new BusinessException(502, "支付渠道未返回有效收银台，请核对渠道下单结果");
        return YeepayUnifiedOrderRes.builder()
                .uniqueOrderNo(extractUniqueOrderNo(result))
                .cashierUrl(extractCashierUrl(result))
                .build();
    }

    private boolean isSafeCashierUrl(String url) {
        try {
            var uri = java.net.URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (Exception ignored) { return false; }
    }

    private void validateRequest(YeepayUnifiedOrderReq request) {
        if (request == null) {
            throw new BusinessException("下单请求不能为空");
        }
        if (request.getOrderAmount() == null || request.getOrderAmount().signum() <= 0) {
            throw new BusinessException("订单金额必须大于 0");
        }
        if (StrUtil.isBlank(request.getPaymentOrderNo())) {
            throw new BusinessException("支付订单号不能为空");
        }
        if (StrUtil.isBlank(request.getGoodsName())) {
            throw new BusinessException("商品名称不能为空");
        }
        if (StrUtil.isBlank(yeepayProperties.getMerchantNo())) {
            throw new BusinessException("易宝 merchantNo 未配置");
        }
        if (StrUtil.isBlank(yeepayDynamicProperties.getNotifyUrl())) {
            throw new BusinessException("易宝 notifyUrl 未配置");
        }
        if (StrUtil.isBlank(yeepayDynamicProperties.getReturnUrl())) {
            throw new BusinessException("易宝 returnUrl 未配置");
        }
        if (yeepayDynamicProperties.getExpireMinutes() == null || yeepayDynamicProperties.getExpireMinutes() <= 0) {
            throw new BusinessException("易宝 expireMinutes 配置不合法");
        }
    }

    private String resolveParentMerchantNo() {
        return StrUtil.blankToDefault(trimToNull(yeepayProperties.getParentMerchantNo()), yeepayProperties.getMerchantNo().trim());
    }

    private String buildMemo(YeepayUnifiedOrderReq request) {
        return yeepayProperties.getMemoPrefix() + ":" + request.getLocalPaymentOrderId();
    }

    private String getNotifyUrl() {
        return yeepayDynamicProperties.getNotifyUrl().trim();
    }

    private String buildReturnUrl(YeepayUnifiedOrderReq request) {
        String baseUrl = yeepayDynamicProperties.getReturnUrl().trim();
        String delimiter = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + delimiter + "orderId=" + request.getBusinessOrderId()
                + "&paymentOrderNo=" + request.getPaymentOrderNo().trim();
    }

    private Integer getExpireMinutes() {
        return yeepayDynamicProperties.getExpireMinutes();
    }

    private String extractCashierUrl(JSONObject result) {
        if (result == null || result.isEmpty()) {
            return null;
        }
        String[] keys = {"cashierUrl", "cashierUrlPage", "redirectUrl", "payUrl"};
        for (String key : keys) {
            String value = result.getString(key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String extractUniqueOrderNo(JSONObject result) {
        if (result == null || result.isEmpty()) {
            return null;
        }
        String[] keys = {"uniqueOrderNo"};
        for (String key : keys) {
            String value = result.getString(key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return value.trim();
    }
}
