package cn.yq.modules.auth.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.yq.config.AuthProperties;
import cn.yq.modules.system.domain.entity.SysUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类。
 * 基于 Hutool JWT 生成与校验登录令牌。
 */
@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    private final AuthProperties authProperties;

    public String createToken(SysUserEntity user) {
        long now = System.currentTimeMillis();
        long expireAt = now + authProperties.getExpireSeconds() * 1000;

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", user.getId());
        payload.put("username", user.getUsername());
        payload.put("nickname", user.getNickname());
        payload.put("iat", new Date(now));
        payload.put("exp", new Date(expireAt));
        return JWTUtil.createToken(payload, getSecretBytes());
    }

    public boolean validateToken(String token) {
        try {
            JWT jwt = JWT.of(token).setKey(getSecretBytes());
            return jwt.verify() && jwt.validate(0);
        } catch (Exception ex) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Object value = JWT.of(token).setKey(getSecretBytes()).getPayload("userId");
        return value == null ? null : Long.parseLong(value.toString());
    }

    public String getUsername(String token) {
        Object value = JWT.of(token).setKey(getSecretBytes()).getPayload("username");
        return value == null ? null : value.toString();
    }

    private byte[] getSecretBytes() {
        return authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
    }
}
