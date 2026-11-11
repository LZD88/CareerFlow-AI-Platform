package cn.yq.config;

import cn.yq.common.dataConfig.service.SystemConfig;
import cn.yq.common.dataConfig.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class YeepayDynamicProperties {

    private final SystemConfigService systemConfigService;

    public Boolean getEnabled() {
        return systemConfigService.get(SystemConfig.YEEPAY_ENABLED);
    }

    public Boolean getUseSandbox() {
        return systemConfigService.get(SystemConfig.YEEPAY_USE_SANDBOX);
    }

    public String getNotifyUrl() {
        return systemConfigService.get(SystemConfig.YEEPAY_NOTIFY_URL);
    }

    public String getReturnUrl() {
        return systemConfigService.get(SystemConfig.YEEPAY_RETURN_URL);
    }

    public Integer getExpireMinutes() {
        return systemConfigService.get(SystemConfig.YEEPAY_EXPIRE_MINUTES);
    }

    public String getLimitPayType() {
        return systemConfigService.get(SystemConfig.YEEPAY_LIMIT_PAY_TYPE);
    }
}
