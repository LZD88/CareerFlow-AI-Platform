package cn.yq.modules.studentportal.interceptor;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.studentportal.context.StudentPortalAccessContextHolder;
import cn.yq.modules.studentportal.domain.StudentPortalAccessContext;
import cn.yq.modules.studentportal.util.StudentPortalJwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class StudentPortalJwtAuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final StudentPortalJwtTokenUtil studentPortalJwtTokenUtil;

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
        if (!studentPortalJwtTokenUtil.validateToken(token)) {
            throw new BusinessException(401, "Token无效或已过期");
        }

        StudentPortalAccessContext accessContext = new StudentPortalAccessContext();
        accessContext.setStudentId(studentPortalJwtTokenUtil.getStudentId(token));
        accessContext.setStudentCode(studentPortalJwtTokenUtil.getStudentCode(token));
        accessContext.setStudentName(studentPortalJwtTokenUtil.getStudentName(token));
        accessContext.setScope(studentPortalJwtTokenUtil.getScope(token));
        accessContext.setOrderId(studentPortalJwtTokenUtil.getOrderId(token));
        StudentPortalAccessContextHolder.set(accessContext);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        StudentPortalAccessContextHolder.clear();
    }
}
