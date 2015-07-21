package com.devicehive.auth.rest;

import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SimpleCORSFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT, DELETE, HEAD");
        resp.setHeader("Access-Control-Max-Age", "0");
        resp.setHeader("Access-Control-Allow-Headers", "Origin, Authorization, Accept, Content-Type, Auth-DeviceID, Auth-DeviceKey");
        chain.doFilter(request, resp);
    }
}
