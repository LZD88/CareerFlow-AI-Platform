package cn.yq.common.redis;

/**
 * Redis Key 常量统一定义。
 *
 * <p>当前项目实际在业务代码中使用到的 Redis Key 主要包括：
 * 1. 登录验签 secret 缓存；
 * 2. 登录验签 nonce 防重放缓存；
 * 3. 学生端登录验签 secret / nonce；
 * 4. 学生支付下单分布式锁；
 * 5. 学生 AI 会话摘要压缩分布式锁。</p>
 *
 * <p>后续如果新增缓存、锁、计数器等 Redis 用途，也建议统一补到这里。</p>
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /**
     * 后台用户请求验签 secret：
     * auth:sign:secret:{userId}
     *
     * <p>用户登录成功后生成短期有效 secret，前端后续请求按该 secret 进行签名，
     * 服务端从 Redis 中读取该值完成验签。</p>
     */
    public static final String AUTH_SIGN_SECRET_PREFIX = "auth:sign:secret:";

    /**
     * 后台用户请求验签 nonce：
     * auth:sign:nonce:{userId}:{nonce}
     *
     * <p>用于防止签名请求重放，nonce 写入 Redis 后在过期时间内不可重复使用。</p>
     */
    public static final String AUTH_SIGN_NONCE_PREFIX = "auth:sign:nonce:";

    /**
     * 学生端请求验签 secret：
     * student:portal:sign:secret:{studentCode}
     *
     * <p>学生登录成功后生成短期有效 secret，学生端后续请求按该 secret 进行签名。</p>
     */
    public static final String STUDENT_PORTAL_SIGN_SECRET_PREFIX = "student:portal:sign:secret:";

    /**
     * 学生端请求验签 nonce：
     * student:portal:sign:nonce:{studentCode}:{nonce}
     *
     * <p>用于防止学生端签名请求重放。</p>
     */
    public static final String STUDENT_PORTAL_SIGN_NONCE_PREFIX = "student:portal:sign:nonce:";

    /**
     * 学生支付创建分布式锁：
     * student:payment:create:{orderNo}
     *
     * <p>用于避免同一个业务订单被重复创建支付单、重复发起收银台下单。</p>
     */
    public static final String STUDENT_PAYMENT_CREATE_LOCK_PREFIX = "student:payment:create:";

    /**
     * 学生 AI 会话摘要压缩锁：
     * student:ai:summary:compress:{sessionId}
     *
     * <p>用于避免同一会话被多个异步任务同时压缩，导致 summary 被覆盖或重复调用模型。</p>
     */
    public static final String STUDENT_AI_SUMMARY_COMPRESS_LOCK_PREFIX = "student:ai:summary:compress:";

    public static String authSignSecretKey(Long userId) {
        return AUTH_SIGN_SECRET_PREFIX + userId;
    }

    public static String authSignNonceKey(Long userId, String nonce) {
        return AUTH_SIGN_NONCE_PREFIX + userId + ":" + nonce;
    }

    public static String studentPortalSignSecretKey(String studentCode) {
        return STUDENT_PORTAL_SIGN_SECRET_PREFIX + studentCode;
    }

    public static String studentPortalSignNonceKey(String studentCode, String nonce) {
        return STUDENT_PORTAL_SIGN_NONCE_PREFIX + studentCode + ":" + nonce;
    }

    public static String studentPaymentCreateLockKey(String orderNo) {
        return STUDENT_PAYMENT_CREATE_LOCK_PREFIX + orderNo;
    }

    public static String studentAiSummaryCompressLockKey(Long sessionId) {
        return STUDENT_AI_SUMMARY_COMPRESS_LOCK_PREFIX + sessionId;
    }
}
