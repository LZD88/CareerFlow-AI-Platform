package cn.yq.modules.studentportal.util;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.yq.config.MockInterviewProperties;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MockInterviewRealtimeTokenUtil {

    private final MockInterviewProperties mockInterviewProperties;

    public String createToken(Long sessionId,
                              Long studentId,
                              String studentCode,
                              String interviewRole,
                              String volcSessionId,
                              Instant expiresAt) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "YQ-MOCK-INTERVIEW");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("studentId", studentId);
        payload.put("studentCode", studentCode);
        payload.put("interviewRole", interviewRole);
        payload.put("volcSessionId", volcSessionId);
        payload.put("exp", expiresAt.getEpochSecond());

        String headerPart = base64Url(JSON.toJSONString(header));
        String payloadPart = base64Url(JSON.toJSONString(payload));
        String signingInput = headerPart + "." + payloadPart;
        return signingInput + "." + sign(signingInput);
    }

    private String sign(String signingInput) {
        HMac hmac = new HMac(
                HmacAlgorithm.HmacSHA256,
                mockInterviewProperties.getRealtimeTokenSecret().getBytes(StandardCharsets.UTF_8)
        );
        byte[] digest = hmac.digest(signingInput.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
