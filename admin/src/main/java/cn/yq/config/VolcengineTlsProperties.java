package cn.yq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yq.volcengine.tls")
public class VolcengineTlsProperties {

    /**
     * 火山 TLS OpenAPI endpoint，例如 tls-cn-beijing.volces.com。
     */
    private String endpoint;

    /**
     * 区域，例如 cn-beijing。
     */
    private String region = "cn-beijing";

    private String accessKeyId;

    private String accessKeySecret;


    private Integer readTimeoutMs = 10000;

    private Integer connectTimeoutMs = 5000;
}
