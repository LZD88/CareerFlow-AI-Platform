package cn.yq.integration.yeepay.callback.config;

import cn.yq.integration.yeepay.callback.YeepayCallbackServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class YeepayCallbackConfig {

    @Bean
    public ServletRegistrationBean<YeepayCallbackServlet> yeepayCallbackServletRegistration(cn.yq.integration.yeepay.service.YeepayReadiness readiness) {
        ServletRegistrationBean<YeepayCallbackServlet> registration =
                new ServletRegistrationBean<>(new YeepayCallbackServlet(), YeepayCallbackServlet.CALLBACK_PREFIX + "/*");
        registration.setName("yeepayCallbackServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }
}
