package com.devicehive.controller.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class OverrideMethodFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(OverrideMethodFilter.class);

    private static final String[] allowedMethods = { HttpMethod.PUT, HttpMethod.GET};
    private static final String overrideHeader = "X-HTTP-Method-Override";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (HttpMethod.POST.equals(requestContext.getMethod())) {
            String method = requestContext.getHeaderString(overrideHeader);
            for (String allowed : allowedMethods) {
                if (allowed.equalsIgnoreCase(method)) {
                    logger.debug("Overriding POST with " + method);
                    requestContext.setMethod(method);
                    break;
                }
            }
        }
    }
}
