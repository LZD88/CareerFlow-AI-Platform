package cn.yq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yq.mock-interview")
public class MockInterviewProperties {

    /**
     * Java 签发、Python 校验的实时面试短期 token 密钥。
     */
    private String realtimeTokenSecret = "change-me-mock-interview-realtime-token-secret";

    /**
     * 实时面试 token 有效期，单位秒。
     */
    private Long realtimeTokenExpireSeconds = 600L;

    /**
     * 前端连接 Python 实时面试网关的 WebSocket 地址。
     */
    private String pythonRealtimeWsUrl = "ws://127.0.0.1:9000/api/mock-interview/realtime";
}
