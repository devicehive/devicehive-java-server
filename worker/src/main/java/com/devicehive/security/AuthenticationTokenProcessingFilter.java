package com.devicehive.security;

import com.devicehive.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

/**
 * Created by tatyana on 3/13/15.
 */
@Component("authenticationTokenProcessingFilter")
public class AuthenticationTokenProcessingFilter extends GenericFilterBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationTokenProcessingFilter.class);
    private static final String AUTH_HEADER = "Authorization";

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;

        final String authHeader = req.getHeader(AUTH_HEADER);
        if (authHeader == null) {
            LOGGER.error("Authentication header not found. Authentication failed.");
            res.sendError(SC_UNAUTHORIZED);
        } else {
            if (authHeader.substring(0, 6).equalsIgnoreCase("Bearer")) {
                final String key = authHeader.substring(6).trim();
                if (authenticationService.authenticate(key) == null) {
                    LOGGER.error("Access key {} not found. Authentication failed.", key);
                    res.sendError(SC_UNAUTHORIZED);
                }
            }
        }
        chain.doFilter(req, res);
    }
}
