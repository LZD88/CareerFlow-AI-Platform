package cn.yq.modules.studentportal.util;

import cn.yq.common.redis.RedisKeyConstants;
import cn.yq.common.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentPortalSignKeyRedisUtil {

    private final RedisService redisService;

    public void cacheStudentSecret(String studentCode, String secret, long expireSeconds) {
        redisService.set(buildSecretKey(studentCode), secret, expireSeconds);
    }

    public String getStudentSecret(String studentCode) {
        return redisService.getKeyOrNull(buildSecretKey(studentCode));
    }

    public boolean saveNonce(String studentCode, String nonce, long expireSeconds) {
        Boolean result = redisService.setIfAbsent(buildNonceKey(studentCode, nonce), nonce, expireSeconds);
        return Boolean.TRUE.equals(result);
    }

    private String buildSecretKey(String studentCode) {
        return RedisKeyConstants.studentPortalSignSecretKey(studentCode);
    }

    private String buildNonceKey(String studentCode, String nonce) {
        return RedisKeyConstants.studentPortalSignNonceKey(studentCode, nonce);
    }
}
