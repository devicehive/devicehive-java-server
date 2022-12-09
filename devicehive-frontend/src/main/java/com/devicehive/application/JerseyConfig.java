package com.devicehive.application;

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

import com.devicehive.application.filter.ContentTypeFilter;
import com.devicehive.resource.impl.ApiInfoResourceImpl;
import com.devicehive.resource.impl.ConfigurationResourceImpl;
import com.devicehive.resource.impl.DeviceCommandResourceImpl;
import com.devicehive.resource.impl.DeviceNotificationResourceImpl;
import com.devicehive.resource.impl.DeviceResourceImpl;
import com.devicehive.resource.impl.DeviceTypeResourceImpl;
import com.devicehive.resource.impl.NetworkResourceImpl;
import com.devicehive.resource.impl.UserResourceImpl;
import com.devicehive.resource.impl.WelcomeResourceImpl;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        registerClasses(
                ApiInfoResourceImpl.class,
                ConfigurationResourceImpl.class,
                DeviceCommandResourceImpl.class,
                DeviceNotificationResourceImpl.class,
                DeviceResourceImpl.class,
                NetworkResourceImpl.class,
                DeviceTypeResourceImpl.class,
                WelcomeResourceImpl.class,
                UserResourceImpl.class
        );

        packages(
                "com.devicehive.resource.exceptions",
                "com.devicehive.resource.filter",
                "com.devicehive.resource.converters"
        );

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        register(RequestContextFilter.class);
        register(LoggingFeature.class);
        register(ContentTypeFilter.class);

        register(ApiListingResource.class);
        register(SwaggerSerializers.class);
    }
}
