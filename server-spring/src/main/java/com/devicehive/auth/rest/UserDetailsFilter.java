package com.devicehive.auth.rest;

import com.devicehive.auth.HiveAuthentication;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetAddress;

public class UserDetailsFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication instanceof AbstractAuthenticationToken) {
                MDC.put("usrinf", authentication.getName());
                HiveAuthentication.HiveAuthDetails details = new HiveAuthentication.HiveAuthDetails(
                        InetAddress.getByName(request.getRemoteAddr()),
                        httpRequest.getHeader(HttpHeaders.ORIGIN),
                        httpRequest.getHeader(HttpHeaders.AUTHORIZATION));
                ((AbstractAuthenticationToken) authentication).setDetails(details);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove("usrinf");
        }
    }
}
