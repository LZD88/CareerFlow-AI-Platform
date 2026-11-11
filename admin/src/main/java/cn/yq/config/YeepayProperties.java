package cn.yq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yq.yeepay")
public class YeepayProperties {

    /**
     * 业务接入商编。
     */
    private String parentMerchantNo;

    /**
     * 收款商编。
     */
    private String merchantNo;

    /** 外置 YOP SDK 配置路径，包含应用 ID、商户私钥及平台验签公钥。 */
    private String sdkConfigFile;

    /**
     * 资金处理方式。
     */
    private String fundProcessType = "REAL_TIME";

    /**
     * 订单备注前缀。
     */
    private String memoPrefix = "student-order";
}
