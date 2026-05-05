package com.springboot.pki.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class H2ConsoleConfig {
    @Bean
    public ServletRegistrationBean<JakartaWebServlet> h2Console() {
        // H2 2.x에서 Jakarta 서블릿을 사용하는 클래스입니다.
        ServletRegistrationBean<JakartaWebServlet> registrationBean = 
            new ServletRegistrationBean<>(new JakartaWebServlet(), "/h2/*");
        registrationBean.setLoadOnStartup(1);
        return registrationBean;
    }
}
