package com.devicehive.application.filter;

import com.devicehive.application.JerseyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

@WebFilter("/swagger")
public class SwaggerFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        URL requestUrl = new URL(request.getRequestURL().toString());
        logger.debug("Swagger filter triggered by '{}: {}'. Request will be redirected to swagger page",
                request.getMethod(), requestUrl);

        String swaggerJsonUrl = String.format("%s://%s:%s%s%s/swagger.json",
                requestUrl.getProtocol(),
                requestUrl.getHost(),
                requestUrl.getPort(),
                request.getContextPath(),
                JerseyConfig.REST_PATH);
        String url = request.getContextPath() + "/swagger.html?url=" + swaggerJsonUrl;

        logger.debug("Request is being redirected to '{}'", url);
        response.sendRedirect(url);
    }
}
