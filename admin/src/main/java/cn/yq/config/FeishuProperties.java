package cn.yq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yq.feishu")
public class FeishuProperties {

    private String appId;

    private String appSecret;

    private String redirectUri;
}
