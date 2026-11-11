package cn.yq.modules.studentportal.interceptor;

import cn.yq.common.exception.BusinessException;
import cn.yq.config.SignProperties;
import cn.yq.modules.auth.filter.CachedBodyHttpServletRequest;
import cn.yq.modules.auth.util.RequestSignUtil;
import cn.yq.modules.studentportal.context.StudentPortalAccessContextHolder;
import cn.yq.modules.studentportal.domain.StudentPortalAccessContext;
import cn.yq.modules.studentportal.util.StudentPortalSignKeyRedisUtil;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class StudentPortalSignAuthInterceptor implements HandlerInterceptor {

    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGN = "X-Sign";

    private final SignProperties signProperties;
    private final StudentPortalSignKeyRedisUtil studentPortalSignKeyRedisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        // SSE 会触发 Servlet 容器的二次 async dispatch。
        // 第二次进入拦截器时仍然带着同一组签名头和 nonce，如果继续防重校验，
        // 会把同一次流式请求误判为重复提交。
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            return true;
        }

        StudentPortalAccessContext accessContext = StudentPortalAccessContextHolder.get();
        String loginStudentCode = accessContext == null ? null : accessContext.getStudentCode();
        if (isBlank(loginStudentCode)) {
            throw new BusinessException(401, "登录状态已失效");
        }

        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String sign = request.getHeader(HEADER_SIGN);
        if (isBlank(timestamp) || isBlank(nonce) || isBlank(sign)) {
            throw new BusinessException(401, "请求签名缺失");
        }

        long requestTimestamp;
        try {
            requestTimestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw new BusinessException(401, "请求时间戳格式错误");
        }

        long now = System.currentTimeMillis();
        long allowedSkewMillis = signProperties.getTimestampSkewSeconds() * 1000;
        if (Math.abs(now - requestTimestamp) > allowedSkewMillis) {
            throw new BusinessException(401, "请求已过期，请重新发起");
        }

        String secret = studentPortalSignKeyRedisUtil.getStudentSecret(loginStudentCode);
        if (isBlank(secret)) {
            throw new BusinessException(401, "验签密钥已失效，请重新登录");
        }

        String requestBody = request instanceof CachedBodyHttpServletRequest cachedRequest
                ? cachedRequest.getCachedBody()
                : "";
        String payload = RequestSignUtil.buildPayload(request, requestBody);
        String signText = RequestSignUtil.buildSignText(
                timestamp,
                nonce,
                request.getMethod(),
                request.getRequestURI(),
                payload
        );
        String expectedSign = RequestSignUtil.sign(signText, secret);
        if (!expectedSign.equalsIgnoreCase(sign)) {
            throw new BusinessException(401, "请求验签失败");
        }

        boolean nonceSaved = studentPortalSignKeyRedisUtil.saveNonce(
                loginStudentCode,
                nonce,
                signProperties.getNonceExpireSeconds()
        );
        if (!nonceSaved) {
            throw new BusinessException(409, "请求重复，请勿重复提交");
        }
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
