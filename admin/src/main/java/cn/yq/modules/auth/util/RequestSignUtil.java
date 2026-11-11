package cn.yq.modules.auth.util;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 请求签名工具。
 */
public final class RequestSignUtil {

    private RequestSignUtil() {
    }

    public static String buildSignText(String timestamp, String nonce, String method, String path, String payload) {
        return String.join("\n",
                defaultString(timestamp),
                defaultString(nonce),
                defaultString(method).toUpperCase(),
                defaultString(path),
                normalizePayload(payload));
    }

    public static String sign(String signText, String secret) {
        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes(StandardCharsets.UTF_8));
        return hMac.digestHex(signText);
    }

    public static String buildPayload(HttpServletRequest request, String requestBody) {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return canonicalizeQuery(request.getQueryString());
        }
        // multipart/form-data 中包含 boundary 和二进制文件内容，
        // 前后端很难稳定得到完全一致的原始字符串，因此文件上传场景统一不对 body 做签名。
        if (isMultipartRequest(request.getContentType())) {
            return "";
        }
        return normalizePayload(requestBody);
    }

    public static String canonicalizeQuery(String queryString) {
        if (!StringUtils.hasText(queryString)) {
            return "";
        }

        String[] pairs = queryString.split("&");
        List<String> normalizedPairs = new ArrayList<>();
        for (String pair : pairs) {
            if (StringUtils.hasText(pair)) {
                normalizedPairs.add(pair);
            }
        }
        normalizedPairs.sort(String::compareTo);
        return String.join("&", normalizedPairs);
    }

    public static String normalizePayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return "";
        }

        String trimmed = payload.trim();
        try {
            Object parsed = JSON.parse(trimmed);
            return stringifyJson(parsed);
        } catch (Exception ex) {
            return trimmed;
        }
    }

    private static String stringifyJson(Object value) {
        if (value instanceof JSONObject jsonObject) {
            List<Map.Entry<String, Object>> entries = new ArrayList<>(jsonObject.entrySet());
            entries.sort(Comparator.comparing(Map.Entry::getKey));
            StringBuilder builder = new StringBuilder("{");
            for (int i = 0; i < entries.size(); i++) {
                Map.Entry<String, Object> entry = entries.get(i);
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(JSON.toJSONString(entry.getKey()));
                builder.append(':');
                builder.append(stringifyJson(entry.getValue()));
            }
            builder.append('}');
            return builder.toString();
        }

        if (value instanceof JSONArray jsonArray) {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < jsonArray.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(stringifyJson(jsonArray.get(i)));
            }
            builder.append(']');
            return builder.toString();
        }

        return JSON.toJSONString(value);
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static boolean isMultipartRequest(String contentType) {
        return StringUtils.hasText(contentType)
                && contentType.toLowerCase().startsWith("multipart/form-data");
    }
}
