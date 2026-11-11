package cn.yq.common.aop;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Aspect
@Component
@Slf4j
public class ControllerLogAspect {

    private static final int MAX_LOG_LENGTH = 4000;
    private static final String[] EXCLUDED_URIS = {
            "/api/log-analysis/**"
    };

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Around("execution(* cn.yq..controller..*.*(..))")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
        HttpServletRequest request = getCurrentRequest();

        if (shouldSkip(request)) {
            return joinPoint.proceed();
        }

        log.info("接口开始: uri={}, httpMethod={}, controller={}, args={}",
                request == null ? "-" : request.getRequestURI(),
                request == null ? "-" : request.getMethod(),
                methodName,
                buildArgsLog(joinPoint.getArgs(), signature.getParameterNames()));

        Object result = joinPoint.proceed();
        long cost = System.currentTimeMillis() - start;
        log.info("接口结束: uri={}, controller={}, cost={}ms, result={}",
                request == null ? "-" : request.getRequestURI(),
                methodName,
                cost,
                toJson(sanitizeObject(result)));
        return result;
    }

    private boolean shouldSkip(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String pathWithinApplication = requestUri;
        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            pathWithinApplication = requestUri.substring(contextPath.length());
        }
        for (String pattern : EXCLUDED_URIS) {
            if (antPathMatcher.match(pattern, pathWithinApplication)) {
                return true;
            }
        }
        return false;
    }

    private HttpServletRequest getCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getRequest();
    }

    private String buildArgsLog(Object[] args, String[] parameterNames) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (shouldIgnore(arg)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i);
            item.put("value", sanitizeObject(arg));
            items.add(item);
        }
        return toJson(items);
    }

    private boolean shouldIgnore(Object arg) {
        return arg == null
                || arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof MultipartFile;
    }

    private Object sanitizeObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof MultipartFile file) {
            return Map.of(
                    "fileName", file.getOriginalFilename(),
                    "size", file.getSize()
            );
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean || value.getClass().isEnum()) {
            return value;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(sanitizeObject(Array.get(value, i)));
            }
            return list;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> list = new ArrayList<>();
            for (Object item : iterable) {
                list.add(sanitizeObject(item));
            }
            return list;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                sanitized.put(key, isSensitiveField(key) ? mask(String.valueOf(entry.getValue())) : sanitizeObject(entry.getValue()));
            }
            return sanitized;
        }
        if (shouldUseDirectString(value.getClass())) {
            return String.valueOf(value);
        }

        Map<String, Object> sanitized = new LinkedHashMap<>();
        Class<?> current = value.getClass();
        while (current != null && current != Object.class) {
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(value);
                    sanitized.put(field.getName(), isSensitiveField(field.getName()) ? mask(fieldValue == null ? null : String.valueOf(fieldValue)) : sanitizeObject(fieldValue));
                } catch (Exception ignored) {
                    sanitized.put(field.getName(), "[unavailable]");
                }
            }
            current = current.getSuperclass();
        }
        return sanitized;
    }

    private boolean shouldUseDirectString(Class<?> clazz) {
        Package currentPackage = clazz.getPackage();
        String packageName = currentPackage == null ? "" : currentPackage.getName();
        return packageName.startsWith("java.")
                || packageName.startsWith("javax.")
                || packageName.startsWith("jakarta.")
                || packageName.startsWith("org.springframework.");
    }

    private boolean isSensitiveField(String fieldName) {
        String lower = fieldName.toLowerCase();
        return lower.contains("password")
                || lower.contains("secret")
                || lower.contains("token");
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() <= 6) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    private String toJson(Object value) {
        try {
            return truncate(JSON.toJSONString(value));
        } catch (Exception ex) {
            return truncate(String.valueOf(value));
        }
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_LOG_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_LOG_LENGTH) + "...(truncated)";
    }
}
