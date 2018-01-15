package com.devicehive.application.filter;

/*
 * #%L
 * DeviceHive Frontend Logic
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

import com.devicehive.application.JerseyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

import static com.devicehive.configuration.Constants.X_FORWARDED_PORT_HEADER_NAME;
import static com.devicehive.configuration.Constants.X_FORWARDED_PROTO_HEADER_NAME;

@WebFilter("/swagger")
public class SwaggerFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerFilter.class);

    @Value("${swagger.protocol:http}")
    private String swaggerProtocol;

    @Value("${swagger.port:80}")
    private String swaggerPort;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String xForwardedProto = request.getHeader(X_FORWARDED_PROTO_HEADER_NAME);
        final String xForwardedPort = request.getHeader(X_FORWARDED_PORT_HEADER_NAME);

        if (xForwardedProto != null) {
            swaggerProtocol = xForwardedProto;
        }

        if (xForwardedPort != null) {
            swaggerPort = xForwardedPort;
        }

        logger.debug("swagger.protocol: {}", swaggerProtocol);
        logger.debug("swagger.port: {}", swaggerPort);

        URL requestUrl = new URL(request.getRequestURL().toString());
        logger.debug("Swagger filter triggered by '{}: {}'. Request will be redirected to swagger page",
                request.getMethod(), requestUrl);

        String swaggerJsonUrl = String.format("%s://%s:%s%s%s/swagger.json",
                swaggerProtocol,
                requestUrl.getHost(),
                swaggerPort,
                request.getContextPath(),
                JerseyConfig.REST_PATH);
        String url = request.getContextPath() + "/swagger.html?url=" + swaggerJsonUrl;

        logger.debug("Request is being redirected to '{}'", url);
        response.sendRedirect(url);
    }
}
