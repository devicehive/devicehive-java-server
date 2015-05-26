package com.devicehive.application.servlets;

import com.devicehive.servlet.InfoServlet;
import com.devicehive.servlet.OAuthLoginServlet;
import com.devicehive.servlet.OAuthServlet;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomServletsConfig {

    @Bean
    public ServletRegistrationBean infoServlet() {
        return new ServletRegistrationBean(new InfoServlet(), "/index");
    }

    @Bean
    public ServletRegistrationBean oAuthLoginServlet() {
        return new ServletRegistrationBean(new OAuthLoginServlet(), "/login", "/home");
    }

    @Bean
    public ServletRegistrationBean oAuthServlet() {
        return new ServletRegistrationBean(new OAuthServlet(), "/oauth2");
    }
}
