package cn.yq.config;

import cn.yq.modules.auth.interceptor.JwtAuthInterceptor;
import cn.yq.modules.auth.interceptor.PermissionAuthInterceptor;
import cn.yq.modules.auth.interceptor.SignAuthInterceptor;
import cn.yq.modules.studentportal.interceptor.StudentPortalJwtAuthInterceptor;
import cn.yq.modules.studentportal.interceptor.StudentPortalPermissionInterceptor;
import cn.yq.modules.studentportal.interceptor.StudentPortalSignAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC配置。
 * 注册登录鉴权拦截器并放行登录与文档接口。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final SignAuthInterceptor signAuthInterceptor;
    private final PermissionAuthInterceptor permissionAuthInterceptor;
    private final StudentPortalJwtAuthInterceptor studentPortalJwtAuthInterceptor;
    private final StudentPortalSignAuthInterceptor studentPortalSignAuthInterceptor;
    private final StudentPortalPermissionInterceptor studentPortalPermissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/feishu-login",
                        "/student-api/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                );

        registry.addInterceptor(signAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/feishu-login",
                        "/student-api/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                );

        registry.addInterceptor(permissionAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/feishu-login",
                        "/api/auth/profile",
                        "/api/auth/change-password",
                        "/api/auth/feishu/bind",
                        "/student-api/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                );

        registry.addInterceptor(studentPortalJwtAuthInterceptor)
                .addPathPatterns("/student-api/**")
                .excludePathPatterns(
                        "/student-api/auth/login",
                        "/student-api/public/orders/*/payment-return-status",
                        "/student-api/public/orders/*/checkout-status"
                );

        registry.addInterceptor(studentPortalSignAuthInterceptor)
                .addPathPatterns("/student-api/**")
                .excludePathPatterns(
                        "/student-api/auth/login",
                        "/student-api/public/orders/*/payment-return-status",
                        "/student-api/public/orders/*/checkout-status"
                );

        registry.addInterceptor(studentPortalPermissionInterceptor)
                .addPathPatterns("/student-api/**")
                .excludePathPatterns(
                        "/student-api/auth/login",
                        "/student-api/public/orders/*/payment-return-status",
                        "/student-api/public/orders/*/checkout-status"
                );
    }
}
