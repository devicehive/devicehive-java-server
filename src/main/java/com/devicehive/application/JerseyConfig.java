package com.devicehive.application;

import com.devicehive.resource.impl.*;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.ApplicationPath;

@Configuration
@ApplicationPath(JerseyConfig.REST_PATH)
public class JerseyConfig extends ResourceConfig {
    public static final String REST_PATH = "/rest";

    public JerseyConfig() {
        packages("com.devicehive.resource.converters", "com.devicehive.resource.exceptions", "com.devicehive.resource.filter");

        /**
         * Concrete Resource classes should be registered manually (not through {@link ResourceConfig#packages(String...)} resource scan)
         * in order to allow to use {@link javax.ws.rs.Path} annotation on interfaces, not implementations.
         * This is described in issue <a href="https://java.net/jira/browse/JERSEY-2591">JERSEY-2591</a>
         */
        registerClasses(AccessKeyResourceImpl.class,
                ApiInfoResourceImpl.class,
                AuthAccessKeyResourceImpl.class,
                ConfigurationResourceImpl.class,
                DeviceClassResourceImpl.class,
                DeviceCommandResourceImpl.class,
                DeviceNotificationResourceImpl.class,
                DeviceResourceImpl.class,
                EquipmentResourceImpl.class,
                NetworkResourceImpl.class,
                OAuthClientResourceImpl.class,
                OAuthGrantResourceImpl.class,
                OAuthTokenResourceImpl.class,
                UserResourceImpl.class);

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        register(RequestContextFilter.class);
        register(LoggingFilter.class);

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
    }

}
