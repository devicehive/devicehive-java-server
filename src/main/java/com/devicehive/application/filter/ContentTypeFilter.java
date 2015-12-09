package com.devicehive.application.filter;

import org.springframework.http.HttpHeaders;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@PreMatching
public class ContentTypeFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if(!requestContext.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE)){
            requestContext.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        }
    }
}
