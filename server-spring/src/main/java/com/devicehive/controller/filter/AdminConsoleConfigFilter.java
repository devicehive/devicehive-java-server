package com.devicehive.controller.filter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AdminConsoleConfigFilter extends GenericFilterBean {

    private static final String CONFIG_TEMPLATE =
            "app.config = { restEndpoint: '%s/rest', rootUrl: '%s', pushState: false };";

    private Environment env;

    public AdminConsoleConfigFilter(Environment env) {
        this.env = env;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String resourcePath = new UrlPathHelper().getPathWithinApplication(httpRequest);

        if (StringUtils.isNoneBlank(resourcePath) && resourcePath.endsWith("config.js")) {
            String contextPath = env.getProperty("server.context-path");
            httpResponse.setContentType("application/javascript;charset=UTF-8");
            httpResponse.getWriter().write(String.format(CONFIG_TEMPLATE, contextPath, contextPath));
        } else {
            chain.doFilter(request, response);
        }

    }

}
