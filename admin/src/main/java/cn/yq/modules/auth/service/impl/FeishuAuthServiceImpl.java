package cn.yq.modules.auth.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.service.authen.v1.model.GetUserInfoResp;
import cn.yq.common.exception.BusinessException;
import cn.yq.config.FeishuProperties;
import cn.yq.modules.auth.domain.vo.res.FeishuUserInfo;
import cn.yq.modules.auth.service.FeishuAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeishuAuthServiceImpl implements FeishuAuthService {

    private static final String FEISHU_USER_ACCESS_TOKEN_URL = "https://open.feishu.cn/open-apis/authen/v2/oauth/token";
    private static final int TIMEOUT = 5000;

    private final FeishuProperties feishuProperties;

    @Override
    public FeishuUserInfo getUserInfoByCode(String code) {
        log.info("飞书绑定开始: code={}, redirectUri={}", mask(code), feishuProperties.getRedirectUri());
        String accessToken = exchangeUserAccessToken(code);
        return getUserInfo(accessToken);
    }

    private String exchangeUserAccessToken(String code) {
        checkConfig();
        Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", code);
        params.put("client_id", feishuProperties.getAppId());
        params.put("client_secret", feishuProperties.getAppSecret());
        params.put("redirect_uri", feishuProperties.getRedirectUri());

        log.info("请求飞书 user_access_token: url={}, params={}",
                FEISHU_USER_ACCESS_TOKEN_URL,
                JSON.toJSONString(maskTokenParams(params)));

        HttpResponse response;
        try {
            response = HttpRequest.post(FEISHU_USER_ACCESS_TOKEN_URL)
                    .form(params)
                    .timeout(TIMEOUT)
                    .execute();
        } catch (Exception ex) {
            log.error("调用飞书获取 user_access_token 失败", ex);
            throw new BusinessException(500, "调用飞书 user_access_token 接口失败");
        }

        log.info("飞书 user_access_token 响应: status={}, body={}", response.getStatus(), response.body());

        if (response.getStatus() != HttpStatus.HTTP_OK) {
            log.error("调用飞书获取 user_access_token 状态异常: status={}, body={}", response.getStatus(), response.body());
            throw new BusinessException(500, "调用飞书 user_access_token 接口失败");
        }

        JSONObject json = parseBody(response.body(), "获取飞书 user_access_token 失败");
        String accessToken = json.getString("access_token");
        if (isBlank(accessToken)) {
            log.error("飞书 user_access_token 响应异常: {}", response.body());
            throw new BusinessException(400, "飞书未返回有效 user_access_token");
        }
        log.info("飞书 user_access_token 获取成功: accessToken={}", mask(accessToken));
        return accessToken;
    }

    private FeishuUserInfo getUserInfo(String accessToken) {
        checkConfig();
        log.info("请求飞书 userInfo: accessToken={}", mask(accessToken));
        Client client = Client.newBuilder(feishuProperties.getAppId(), feishuProperties.getAppSecret()).build();
        GetUserInfoResp response;
        try {
            response = client.authen().v1().userInfo().get(
                    RequestOptions.newBuilder()
                            .userAccessToken(accessToken)
                            .build()
            );
        } catch (Exception ex) {
            throw new BusinessException(500, "调用飞书用户信息接口失败");
        }

        if (response == null || !response.success() || response.getData() == null) {
            String message = response == null ? "获取飞书用户信息失败" : response.getMsg();
            log.warn("飞书 userInfo 响应异常: success={}, code={}, msg={}",
                    response != null && response.success(),
                    response == null ? null : response.getCode(),
                    response == null ? null : response.getMsg());
            throw new BusinessException(400, isBlank(message) ? "获取飞书用户信息失败" : message);
        }

        if (response.getData().getUnionId() == null || isBlank(response.getData().getUnionId())) {
            throw new BusinessException(400, "飞书未返回 union_id");
        }

        log.info("飞书 userInfo 响应成功: openId={}, unionId={}, userId={}, name={}",
                mask(response.getData().getOpenId()),
                mask(response.getData().getUnionId()),
                mask(response.getData().getUserId()),
                response.getData().getName());

        FeishuUserInfo userInfo = new FeishuUserInfo();
        userInfo.setAccessToken(accessToken);
        userInfo.setOpenId(response.getData().getOpenId());
        userInfo.setUnionId(response.getData().getUnionId());
        userInfo.setUserId(response.getData().getUserId());
        userInfo.setName(response.getData().getName());
        userInfo.setEmail(response.getData().getEmail());
        return userInfo;
    }

    private JSONObject parseBody(String body, String defaultMessage) {
        JSONObject json;
        try {
            json = JSON.parseObject(body);
        } catch (Exception ex) {
            throw new BusinessException(500, defaultMessage);
        }

        Integer code = json.getInteger("code");
        if (code != null && code != 0) {
            throw new BusinessException(400, json.getString("msg"));
        }
        return json;
    }

    private void checkConfig() {
        if (isBlank(feishuProperties.getAppId()) || isBlank(feishuProperties.getAppSecret())) {
            throw new BusinessException(500, "飞书应用配置不完整");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Map<String, Object> maskTokenParams(Map<String, Object> params) {
        Map<String, Object> masked = new HashMap<>(params);
        masked.put("code", mask((String) masked.get("code")));
        masked.put("client_id", mask((String) masked.get("client_id")));
        masked.put("client_secret", mask((String) masked.get("client_secret")));
        return masked;
    }

    private String mask(String value) {
        if (isBlank(value)) {
            return value;
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
