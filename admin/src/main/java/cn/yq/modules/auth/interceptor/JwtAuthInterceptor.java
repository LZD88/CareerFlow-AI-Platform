package cn.yq.modules.auth.interceptor;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.auth.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT登录校验拦截器。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader(AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(401, "未登录或Token缺失");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!jwtTokenUtil.validateToken(token)) {
            throw new BusinessException(401, "Token无效或已过期");
        }

        request.setAttribute("loginUserId", jwtTokenUtil.getUserId(token));
        request.setAttribute("loginUsername", jwtTokenUtil.getUsername(token));
        return true;
    }
}
