package com.devicehive.resource.filter;

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

    private static final String[] allowedMethods =
        {HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.OPTIONS};
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
