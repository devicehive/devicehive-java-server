package com.devicehive.controller.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@PreMatching
public class ReplacePostMethodFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ReplacePostMethodFilter.class);

    private static final String[] allowedMethods = {HttpMethod.PUT, HttpMethod.DELETE};
    private static final String overrideHeader = "X-HTTP-Method-Override";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (HttpMethod.POST.equalsIgnoreCase(requestContext.getMethod())) {
            String method = requestContext.getHeaderString(overrideHeader);
            for (String allowed : allowedMethods) {
                if (allowed.equalsIgnoreCase(method)) {
                    logger.debug("Overriding POST method with " + allowed);
                    requestContext.setMethod(allowed);
                    break;
                }
            }
        }
    }
}
