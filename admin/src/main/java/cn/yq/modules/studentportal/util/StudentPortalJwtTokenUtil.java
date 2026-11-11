package cn.yq.modules.studentportal.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.yq.config.AuthProperties;
import cn.yq.modules.studentportal.domain.StudentPortalTokenClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StudentPortalJwtTokenUtil {

    private static final String TOKEN_TYPE = "STUDENT_PORTAL";

    private final AuthProperties authProperties;

    public String createToken(StudentPortalTokenClaims claims) {
        long now = System.currentTimeMillis();
        long expireAt = now + authProperties.getExpireSeconds() * 1000;

        Map<String, Object> payload = new HashMap<>();
        payload.put("tokenType", TOKEN_TYPE);
        payload.put("studentId", claims.getStudentId());
        payload.put("studentCode", claims.getStudentCode());
        payload.put("studentName", claims.getStudentName());
        payload.put("scope", claims.getScope());
        payload.put("orderId", claims.getOrderId());
        payload.put("iat", new Date(now));
        payload.put("exp", new Date(expireAt));
        return JWTUtil.createToken(payload, getSecretBytes());
    }

    public boolean validateToken(String token) {
        try {
            JWT jwt = JWT.of(token).setKey(getSecretBytes());
            if (!jwt.verify() || !jwt.validate(0)) {
                return false;
            }
            Object tokenType = jwt.getPayload("tokenType");
            return TOKEN_TYPE.equals(tokenType == null ? null : tokenType.toString());
        } catch (Exception ex) {
            return false;
        }
    }

    public Long getStudentId(String token) {
        Object value = JWT.of(token).setKey(getSecretBytes()).getPayload("studentId");
        return value == null ? null : Long.parseLong(value.toString());
    }

    public String getStudentCode(String token) {
        Object value = JWT.of(token).setKey(getSecretBytes()).getPayload("studentCode");
        return value == null ? null : value.toString();
    }

    public String getStudentName(String token) {
        Object value = JWT.of(token).setKey(getSecretBytes()).getPayload("studentName");
        return value == null ? null : value.toString();
    }

    public String getScope(String token) {
        Object value = JWT.of(token).setKey(getSecretBytes()).getPayload("scope");
        return value == null ? null : value.toString();
    }

    public Long getOrderId(String token) {
        Object value = JWT.of(token).setKey(getSecretBytes()).getPayload("orderId");
        return value == null ? null : Long.parseLong(value.toString());
    }

    private byte[] getSecretBytes() {
        return authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
    }
}
