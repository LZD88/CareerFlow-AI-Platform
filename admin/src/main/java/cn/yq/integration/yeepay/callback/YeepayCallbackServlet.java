package cn.yq.integration.yeepay.callback;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Maps;
import com.yeepay.yop.sdk.YopConstants;
import com.yeepay.yop.sdk.constants.CharacterConstants;
import com.yeepay.yop.sdk.http.YopContentType;
import com.yeepay.yop.sdk.service.common.YopCallbackEngine;
import com.yeepay.yop.sdk.service.common.callback.YopCallbackRequest;
import com.yeepay.yop.sdk.service.common.callback.YopCallbackResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static com.yeepay.yop.sdk.YopConstants.YOP_HTTP_CONTENT_TYPE_JSON;

public class YeepayCallbackServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(YeepayCallbackServlet.class);

    public static final String CALLBACK_PREFIX = "/yop-callback";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            YopCallbackRequest callbackRequest = resolve(req);
            YopCallbackResponse callbackResponse = YopCallbackEngine.handle(callbackRequest);
            writeResponse(resp, callbackResponse);
        } catch (Exception ex) {
            LOGGER.error("error when handle yeepay callback", ex);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private void writeResponse(HttpServletResponse resp, YopCallbackResponse callbackResponse) throws IOException {
        if (callbackResponse == null) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        if (MapUtils.isNotEmpty(callbackResponse.getHeaders())) {
            callbackResponse.getHeaders().forEach(resp::addHeader);
        }
        if (callbackResponse.getContentType() != null) {
            resp.setContentType(callbackResponse.getContentType().getValue());
        }
        String body = StrUtil.nullToEmpty(callbackResponse.getBody());
        resp.getOutputStream().write(body.getBytes(YopConstants.DEFAULT_ENCODING));
    }

    private YopCallbackRequest resolve(HttpServletRequest req) throws IOException {
        String contentTypeStr = req.getContentType();
        Object content = null;
        YopContentType contentType;
        if (StringUtils.startsWith(contentTypeStr, YOP_HTTP_CONTENT_TYPE_JSON)) {
            contentType = YopContentType.JSON;
            content = IOUtils.toString(req.getInputStream(), YopConstants.DEFAULT_ENCODING);
        } else {
            contentType = YopContentType.FORM_URL_ENCODE;
        }
        return new YopCallbackRequest(resolveCallbackType(req), req.getMethod())
                .setContentType(contentType)
                .setHeaders(getHeaders(req))
                .setParams(getParams(req))
                .setContent(content);
    }

    private String resolveCallbackType(HttpServletRequest req) {
        String requestUri = req.getRequestURI();
        if (requestUri.equals(CALLBACK_PREFIX)) {
            return CharacterConstants.SLASH;
        }
        return StringUtils.substringAfter(requestUri, CALLBACK_PREFIX);
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> result = Maps.newHashMap();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            result.put(headerName, request.getHeader(headerName));
        }
        return result;
    }

    private Map<String, List<String>> getParams(HttpServletRequest request) {
        Map<String, List<String>> result = Maps.newHashMap();
        Map<String, String[]> params = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            result.put(entry.getKey(), Arrays.asList(entry.getValue()));
        }
        return result;
    }
}
