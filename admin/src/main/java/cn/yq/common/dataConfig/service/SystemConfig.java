package cn.yq.common.dataConfig.service;

public class SystemConfig {

    private SystemConfig() {
    }

    public static final SystemConfigItem<String> JWT_SECRET_KEY =
            new SystemConfigItem<>("jwtSecretKey", "yq-ai-admin-jwt-secret", String.class);
    public static final SystemConfigItem<Long> JWT_EXPIRE_SECONDS =
            new SystemConfigItem<>("jwtExpireTime", 7200L, Long.class);
    public static final SystemConfigItem<Long> SIGN_SECRET_EXPIRE_SECONDS =
            new SystemConfigItem<>("signSecretExpireSeconds", 7200L, Long.class);
    public static final SystemConfigItem<Long> SIGN_TIMESTAMP_SKEW_SECONDS =
            new SystemConfigItem<>("signTimestampSkewSeconds", 300L, Long.class);
    public static final SystemConfigItem<Long> SIGN_NONCE_EXPIRE_SECONDS =
            new SystemConfigItem<>("signNonceExpireSeconds", 300L, Long.class);
    public static final SystemConfigItem<Boolean> YEEPAY_ENABLED =
            new SystemConfigItem<>("yeepayEnabled", true, Boolean.class);
    public static final SystemConfigItem<Boolean> YEEPAY_USE_SANDBOX =
            new SystemConfigItem<>("yeepayUseSandbox", false, Boolean.class);
    public static final SystemConfigItem<String> YEEPAY_NOTIFY_URL =
            new SystemConfigItem<>("yeepayNotifyUrl", "", String.class);
    public static final SystemConfigItem<String> YEEPAY_RETURN_URL =
            new SystemConfigItem<>("yeepayReturnUrl", "", String.class);
    public static final SystemConfigItem<Integer> YEEPAY_EXPIRE_MINUTES =
            new SystemConfigItem<>("yeepayExpireMinutes", 5, Integer.class);
    public static final SystemConfigItem<String> YEEPAY_LIMIT_PAY_TYPE =
            new SystemConfigItem<>("yeepayLimitPayType", "", String.class);
    public static final SystemConfigItem<String> VOLCENGINE_TLS_DEFAULT_TOPIC_ID =
            new SystemConfigItem<>("volcengineTlsDefaultTopicId", "", String.class);
    public static final SystemConfigItem<Integer> STUDENT_AI_SUMMARY_TRIGGER_UNCOMPRESSED_COUNT =
            new SystemConfigItem<>("studentAiSummaryTriggerUncompressedCount", 14, Integer.class);
    public static final SystemConfigItem<Integer> STUDENT_AI_SUMMARY_KEEP_RECENT_COUNT =
            new SystemConfigItem<>("studentAiSummaryKeepRecentCount", 6, Integer.class);
    public static final SystemConfigItem<Integer> STUDENT_AI_SUMMARY_MAX_TOKENS =
            new SystemConfigItem<>("studentAiSummaryMaxTokens", 800, Integer.class);

}
