package cn.yq.modules.auth.util;

import cn.yq.common.redis.RedisKeyConstants;
import cn.yq.common.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 验签相关的 Redis Key 工具。
 */
@Component
@RequiredArgsConstructor
public class SignKeyRedisUtil {

    private final RedisService redisService;

    public void cacheUserSecret(Long userId, String secret, long expireSeconds) {
        redisService.set(buildSecretKey(userId), secret, expireSeconds);
    }

    public String getUserSecret(Long userId) {
        return redisService.getKeyOrNull(buildSecretKey(userId));
    }

    public boolean saveNonce(Long userId, String nonce, long expireSeconds) {
        Boolean result = redisService.setIfAbsent(buildNonceKey(userId, nonce), nonce, expireSeconds);
        return Boolean.TRUE.equals(result);
    }

    private String buildSecretKey(Long userId) {
        return RedisKeyConstants.authSignSecretKey(userId);
    }

    private String buildNonceKey(Long userId, String nonce) {
        return RedisKeyConstants.authSignNonceKey(userId, nonce);
    }
}
