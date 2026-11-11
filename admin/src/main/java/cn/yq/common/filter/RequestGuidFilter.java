package cn.yq.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 为每个请求分配唯一标识，便于日志追踪和问题排查。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestGuidFilter extends OncePerRequestFilter {

    public static final String REQUEST_GUID_KEY = "guid";
    public static final String REQUEST_GUID_ATTR = "requestGuid";
    public static final String REQUEST_GUID_HEADER = "X-Request-Guid";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String guid = resolveGuid(request);
        MDC.put(REQUEST_GUID_KEY, guid);
        request.setAttribute(REQUEST_GUID_ATTR, guid);
        response.setHeader(REQUEST_GUID_HEADER, guid);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_GUID_KEY);
        }
    }

    private String resolveGuid(HttpServletRequest request) {
        String guid = request.getHeader(REQUEST_GUID_HEADER);
        if (StringUtils.hasText(guid)) {
            return guid.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
