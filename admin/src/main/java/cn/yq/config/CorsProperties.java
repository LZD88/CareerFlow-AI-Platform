package cn.yq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域配置属性。
 */
@Data
@ConfigurationProperties(prefix = "yq.cors")
public class CorsProperties {

    /**
     * 允许访问的前端地址列表。
     */
    private List<String> allowedOrigins = new ArrayList<>();
}
