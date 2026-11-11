package cn.yq.integration.yeepay.service;

import cn.yq.config.YeepayProperties;
import cn.yq.config.YeepayDynamicProperties;
import cn.yq.common.exception.BusinessException;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** 只向客户端返回缺失项，不暴露配置内容。SDK 仅使用显式指定的商户配置。 */
@Component
@RequiredArgsConstructor
public class YeepayReadiness {
    private final YeepayProperties properties;
    private final YeepayDynamicProperties dynamic;
    private boolean sdkConfigured;

    @PostConstruct
    void initialize() {
        if (blank(properties.getSdkConfigFile())) return;
        try {
            Path path = Path.of(properties.getSdkConfigFile()).toAbsolutePath();
            var json = JSON.parseObject(Files.readString(path));
            var keys = json == null ? null : json.getJSONArray("isvPrivateKey");
            boolean privateKeyPresent = keys != null && keys.stream().anyMatch(key -> {
                var cert = JSON.parseObject(JSON.toJSONString(key));
                return cert != null && !blank(cert.getString("value")) && !cert.getString("value").startsWith("REPLACE_");
            });
            sdkConfigured = json != null && !blank(json.getString("appKey")) && !json.getString("appKey").startsWith("REPLACE_")
                    && (privateKeyPresent || (json.getJSONObject("yopCertStore") != null && !json.getJSONObject("yopCertStore").isEmpty()));
            if (sdkConfigured) {
                // SDK 对绝对路径要求 file:// 前缀。
                System.setProperty("yop.sdk.config.file", path.toUri().toString().replaceFirst("^file:/+", "file:///"));
            }
        } catch (Exception ignored) {
            sdkConfigured = false;
        }
    }

    public List<String> missing() {
        var missing = new ArrayList<String>();
        if (!Boolean.TRUE.equals(dynamic.getEnabled())) missing.add("易宝支付开关");
        if (blank(properties.getMerchantNo())) missing.add("易宝商户号");
        if (!sdkConfigured) missing.add("YOP SDK 应用和密钥配置文件");
        if (!publicNotifyUrl(dynamic.getNotifyUrl())) missing.add("支付结果通知地址（公网 HTTP/HTTPS）");
        if (!httpUrl(dynamic.getReturnUrl())) missing.add("付款返回页地址");
        if (dynamic.getExpireMinutes() == null || dynamic.getExpireMinutes() < 1 || dynamic.getExpireMinutes() > 120)
            missing.add("收银台有效期（1—120 分钟）");
        return missing;
    }

    public void requireReady() {
        List<String> missing = missing();
        if (!missing.isEmpty()) throw new BusinessException(503, "暂未开通在线支付，请配置：" + String.join("、", missing));
    }

    private boolean httpUrl(String value) {
        try {
            URI uri = URI.create(value);
            return ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) && uri.getHost() != null;
        } catch (Exception ignored) { return false; }
    }
    private boolean publicNotifyUrl(String value) {
        if (!httpUrl(value)) return false;
        String host = URI.create(value).getHost().toLowerCase(java.util.Locale.ROOT);
        if (host.equals("localhost") || host.equals("[::1]") || host.equals("::1")
                || host.endsWith(".local") || host.endsWith(".localhost")) return false;
        if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            String[] parts = host.split("\\.");
            int first = Integer.parseInt(parts[0]), second = Integer.parseInt(parts[1]);
            return first != 0 && first != 10 && first != 127 && !(first == 192 && second == 168)
                    && !(first == 172 && second >= 16 && second <= 31) && !(first == 169 && second == 254);
        }
        return true;
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
