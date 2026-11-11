package cn.yq.modules.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 为接口请求提供可重复读取的请求体。
 */
@Component
public class CachedBodyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String servletPath = request.getServletPath();
        if (!servletPath.startsWith("/api/") && !servletPath.startsWith("/student-api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        // multipart/form-data 需要由 Spring 自己解析文件流，
        // 这里不能提前读取并包装请求体，否则 @RequestPart("file") 会拿不到上传文件。
        if (isMultipartRequest(request.getContentType())) {
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(new CachedBodyHttpServletRequest(request), response);
    }

    private boolean isMultipartRequest(String contentType) {
        return StringUtils.hasText(contentType)
                && contentType.toLowerCase().startsWith("multipart/form-data");
    }
}
