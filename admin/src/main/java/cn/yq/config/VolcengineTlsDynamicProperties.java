package cn.yq.config;

import cn.yq.common.dataConfig.service.SystemConfig;
import cn.yq.common.dataConfig.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VolcengineTlsDynamicProperties {

    private final SystemConfigService systemConfigService;

    public String getDefaultTopicId() {
        return systemConfigService.get(SystemConfig.VOLCENGINE_TLS_DEFAULT_TOPIC_ID);
    }
}
