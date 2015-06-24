package com.devicehive.application;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.ApplicationPath;

@Configuration
@ApplicationPath("/rest")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        packages("com.devicehive.resource");

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        register(RequestContextFilter.class);
        register(LoggingFilter.class);
    }

}
