package com.devicehive.auth.rest;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.configuration.Constants;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class AuthenticationFilter extends GenericFilterBean {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private AuthenticationManager authenticationManager;

    public AuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        Optional<String> authHeader = Optional.ofNullable(httpRequest.getHeader(HttpHeaders.AUTHORIZATION));

        String resourcePath = new UrlPathHelper().getPathWithinApplication(httpRequest);
        logger.info("Security intercepted request to {}", resourcePath);

        try {
            if (authHeader.isPresent()) {
                String header = authHeader.get();
                if (header.startsWith("Basic")) {
                    processBasicAuth(header);
                }
                HiveAuthentication.HiveAuthDetails details = createUserDetails(httpRequest);
                HiveAuthentication authentication = (HiveAuthentication) SecurityContextHolder.getContext().getAuthentication();
                authentication.setDetails(details);
            }

            chain.doFilter(request, response);
        } catch (InternalAuthenticationServiceException e) {
            SecurityContextHolder.clearContext();
            logger.error("Internal authentication service exception", e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        }
    }

    private HiveAuthentication.HiveAuthDetails createUserDetails(HttpServletRequest request) throws UnknownHostException {
        return new HiveAuthentication.HiveAuthDetails(
                InetAddress.getByName(request.getRemoteAddr()),
                request.getHeader(HttpHeaders.ORIGIN),
                request.getHeader(HttpHeaders.AUTHORIZATION));
    }

    private void processBasicAuth(String authHeader) throws UnsupportedEncodingException {
        Pair<String, String> credentials = extractAndDecodeHeader(authHeader);
        UsernamePasswordAuthenticationToken requestAuth = new UsernamePasswordAuthenticationToken(credentials.getLeft().trim(), credentials.getRight().trim());
        Authentication authentication = authenticationManager.authenticate(requestAuth);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Pair<String, String> extractAndDecodeHeader(String header) throws UnsupportedEncodingException {
        byte[] base64Token = header.substring(6).getBytes("UTF-8");
        byte[] decoded;
        try {
            decoded = Base64.decode(base64Token);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Failed to decode basic authentication token");
        }
        String token = new String(decoded, "UTF-8");
        int delim = token.indexOf(":");
        if (delim == -1) {
            throw new BadCredentialsException("Invalid basic authentication token");
        }
        return Pair.of(token.substring(0, delim), token.substring(delim + 1));
    }

}
