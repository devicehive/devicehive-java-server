package com.devicehive.application;

import com.devicehive.application.filter.ContentTypeFilter;
import com.devicehive.resource.impl.*;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.springframework.stereotype.Component;

import javax.ws.rs.ApplicationPath;

@Component
@ApplicationPath(JerseyConfig.REST_PATH)
public class JerseyConfig extends ResourceConfig {
    public static final String REST_PATH = "/rest";

    public JerseyConfig() {
        packages("com.devicehive.resource.converters",
                "com.devicehive.resource.exceptions",
                "com.devicehive.resource.filter");

        registerClasses(ApiInfoResourceImpl.class,
                AccessKeyResourceImpl.class,
                ConfigurationResourceImpl.class,
                AuthJwtTokenResourceImpl.class,
                DeviceClassResourceImpl.class,
                DeviceCommandResourceImpl.class,
                DeviceNotificationResourceImpl.class,
                DeviceResourceImpl.class,
                EquipmentResourceImpl.class,
                NetworkResourceImpl.class,
                WelcomeResourceImpl.class,
                UserResourceImpl.class,
                JwtTokenResourceImpl.class);

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        register(RequestContextFilter.class);
        register(LoggingFilter.class);
        register(ContentTypeFilter.class);

        register(io.swagger.jaxrs.listing.ApiListingResource.class);
        register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
    }
}
