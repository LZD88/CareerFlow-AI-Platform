package cn.yq.modules.upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "volcengine.tos")
public class TosConfig {

    /**
     * OSS 访问端点
     */
    private String endpoint;

    private String region;

    private String bucketName;

    /**
     * AccessKey ID
     */
    private String accessKey = System.getenv("TOS_ACCESS_KEY");

    private String secretKey = System.getenv("TOS_SECRET_KEY");
}
