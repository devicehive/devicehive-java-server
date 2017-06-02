package com.devicehive.auth.rest;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.auth.HiveAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.UUID;

public class HttpAuthenticationFilter extends GenericFilterBean {

    private static final Logger logger = LoggerFactory.getLogger(HttpAuthenticationFilter.class);

    private AuthenticationManager authenticationManager;

    public HttpAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        Optional<String> authHeader = Optional.ofNullable(httpRequest.getHeader(HttpHeaders.AUTHORIZATION));

        String resourcePath = new UrlPathHelper().getPathWithinApplication(httpRequest);
        logger.debug("Security intercepted request to {}", resourcePath);

        try {
            if (authHeader.isPresent()) {
                if (authHeader.get().length() > 6 && authHeader.get().substring(0,6).equals("Bearer")) {
                    processJwtAuth(authHeader.get().substring(6).trim());
                } else {
                    SecurityContextHolder.clearContext();
                    httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                }
            } else {
                processAnonymousAuth();
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication instanceof AbstractAuthenticationToken) {
                MDC.put("usrinf", authentication.getName());
                HiveAuthentication.HiveAuthDetails details = createUserDetails(httpRequest);
                ((AbstractAuthenticationToken) authentication).setDetails(details);
            }

            chain.doFilter(request, response);
        } catch (InternalAuthenticationServiceException e) {
            SecurityContextHolder.clearContext();
            logger.error("Internal authentication service exception", e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } finally {
            MDC.remove("usrinf");
        }
    }

    private HiveAuthentication.HiveAuthDetails createUserDetails(HttpServletRequest request) throws UnknownHostException {
        return new HiveAuthentication.HiveAuthDetails(
                InetAddress.getByName(request.getRemoteAddr()),
                request.getHeader(HttpHeaders.ORIGIN),
                request.getHeader(HttpHeaders.AUTHORIZATION));
    }

    private void processJwtAuth(String token) {
        PreAuthenticatedAuthenticationToken requestAuth = new PreAuthenticatedAuthenticationToken(token, null);
        tryAuthenticate(requestAuth);
    }

    private void processAnonymousAuth() {
        AnonymousAuthenticationToken requestAuth = new AnonymousAuthenticationToken(UUID.randomUUID().toString(),
                "anonymousUser",  AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        tryAuthenticate(requestAuth);
    }

    private void tryAuthenticate(Authentication requestAuth) {
        Authentication authentication = authenticationManager.authenticate(requestAuth);
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InternalAuthenticationServiceException("Unable to authenticate user with provided credentials");
        }
        logger.debug("Successfully authenticated");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
