package cn.yq.config;

import cn.yq.common.dataConfig.service.SystemConfigService;
import cn.yq.common.dataConfig.service.SystemConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JWT认证配置属性。
 */
@Component
@RequiredArgsConstructor
public class AuthProperties {

    private final SystemConfigService systemConfigService;

    /**
     * JWT签名密钥。
     */
    public String getJwtSecret() {
        return systemConfigService.get(SystemConfig.JWT_SECRET_KEY);
    }

    /**
     * Token有效期，单位秒。
     */
    public Long getExpireSeconds() {
        return systemConfigService.get(SystemConfig.JWT_EXPIRE_SECONDS);
    }
}
