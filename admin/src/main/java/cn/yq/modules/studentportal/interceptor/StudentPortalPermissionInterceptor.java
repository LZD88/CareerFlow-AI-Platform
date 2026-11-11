package cn.yq.modules.studentportal.interceptor;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.context.StudentPortalAccessContextHolder;
import cn.yq.modules.studentportal.domain.StudentPortalAccessContext;
import cn.yq.modules.studentportal.domain.entity.StuPermissionEntity;
import cn.yq.modules.studentportal.service.StudentPortalPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class StudentPortalPermissionInterceptor implements HandlerInterceptor {

    private static final String SCOPE_FULL = "FULL";
    private static final String SCOPE_PAY_ONLY = "PAY_ONLY";
    private static final String SCOPE_PROFILE_ONLY = "PROFILE_ONLY";

    private static final List<String> PAY_ONLY_ALLOW_PATHS = List.of(
            "/student-api/orders/*/payment-preview",
            "/student-api/orders/*/cashier-unified-order",
            "/student-api/orders/*/payment-return-status"
    );

    private static final List<String> PROFILE_ONLY_ALLOW_PATHS = List.of(
            "/student-api/profile",
            "/student-api/orders/*/enrollment-draft",
            "/student-api/orders/*/enrollment"
    );

    private final StudentService studentService;
    private final StudentPortalPermissionService studentPortalPermissionService;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        StudentPortalAccessContext accessContext = StudentPortalAccessContextHolder.get();
        if (accessContext == null || accessContext.getScope() == null || accessContext.getScope().trim().isEmpty()) {
            throw new BusinessException(401, "未登录或登录状态失效");
        }

        String requestUri = request.getServletPath();
        if (SCOPE_PAY_ONLY.equals(accessContext.getScope())) {
            if (!matchesAny(requestUri, PAY_ONLY_ALLOW_PATHS)) {
                throw new BusinessException(403, "当前登录态仅支持支付相关操作");
            }
            return true;
        }

        if (SCOPE_PROFILE_ONLY.equals(accessContext.getScope())) {
            if (!matchesAny(requestUri, PROFILE_ONLY_ALLOW_PATHS)) {
                throw new BusinessException(403, "当前登录态仅支持入学资料相关操作");
            }
            return true;
        }

        if (!SCOPE_FULL.equals(accessContext.getScope())) {
            throw new BusinessException(403, "当前登录态不支持访问该接口");
        }

        if (accessContext.getStudentCode() == null || accessContext.getStudentCode().trim().isEmpty()) {
            throw new BusinessException(401, "未登录或登录状态失效");
        }
        StudentEntity student = studentService.findByStudentCode(accessContext.getStudentCode());
        if (student == null) {
            throw new BusinessException(404, "学员信息不存在");
        }

        boolean matched = studentPortalPermissionService.listApiPermissions(student).stream()
                .map(StuPermissionEntity::getApiPath)
                .filter(Objects::nonNull)
                .filter(path -> !path.isBlank())
                .anyMatch(path -> antPathMatcher.match(path, requestUri));
        if (!matched) {
            throw new BusinessException(403, "无权访问当前接口");
        }
        return true;
    }

    private boolean matchesAny(String requestUri, List<String> patterns) {
        return patterns.stream().anyMatch(pattern -> antPathMatcher.match(pattern, requestUri));
    }
}
