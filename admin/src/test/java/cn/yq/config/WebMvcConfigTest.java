package cn.yq.config;

import cn.yq.modules.auth.interceptor.*;
import cn.yq.modules.studentportal.interceptor.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class WebMvcConfigTest {
    @SuppressWarnings("unchecked")
    List<MappedInterceptor> interceptors() {
        var config = new WebMvcConfig(mock(JwtAuthInterceptor.class), mock(SignAuthInterceptor.class),
                mock(PermissionAuthInterceptor.class), mock(StudentPortalJwtAuthInterceptor.class),
                mock(StudentPortalSignAuthInterceptor.class), mock(StudentPortalPermissionInterceptor.class));
        var registry = new InterceptorRegistry();
        config.addInterceptors(registry);
        return (List<MappedInterceptor>) ReflectionTestUtils.invokeMethod(registry, "getInterceptors");
    }
    long matching(String path) {
        var request = new MockHttpServletRequest("POST", "/yq-admin" + path);
        request.setContextPath("/yq-admin");
        request.setServletPath(path);
        ServletRequestPathUtils.parseAndCache(request);
        return interceptors().stream().filter(item -> item.matches(request)).count();
    }
    @Test void registersBothAdminAndStudentSecurityChains() {
        assertEquals(6, interceptors().size());
        assertEquals(3, matching("/api/sales/orders/1/enrollment"));
        assertEquals(3, matching("/student-api/orders/1/enrollment"));
    }
    @Test void loginAndPublicPaymentReturnRemainPublic() {
        assertEquals(0, matching("/api/auth/login"));
        assertEquals(0, matching("/api/auth/feishu-login"));
        assertEquals(0, matching("/student-api/auth/login"));
        assertEquals(0, matching("/student-api/public/orders/1/payment-return-status"));
    }
    @Test void personalSettingsStillRequireLoginAndSignature() {
        assertEquals(2, matching("/api/auth/profile"));
        assertEquals(2, matching("/api/auth/change-password"));
        assertEquals(2, matching("/api/auth/feishu/bind"));
    }
}
