package cn.yq.config;

import cn.yq.common.dataConfig.service.SystemConfig;
import cn.yq.common.dataConfig.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 请求验签配置。
 */
@Component
@RequiredArgsConstructor
public class SignProperties {

    private final SystemConfigService systemConfigService;

    /**
     * 登录后下发给用户的签名密钥有效期，单位秒。
     */
    public Long getSecretExpireSeconds() {
        return systemConfigService.get(SystemConfig.SIGN_SECRET_EXPIRE_SECONDS);
    }

    /**
     * 时间戳允许的误差范围，单位秒。
     */
    public Long getTimestampSkewSeconds() {
        return systemConfigService.get(SystemConfig.SIGN_TIMESTAMP_SKEW_SECONDS);
    }

    /**
     * 随机串防重放缓存时长，单位秒。
     */
    public Long getNonceExpireSeconds() {
        return systemConfigService.get(SystemConfig.SIGN_NONCE_EXPIRE_SECONDS);
    }
}
