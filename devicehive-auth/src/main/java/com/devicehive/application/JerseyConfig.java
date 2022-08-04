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
import com.devicehive.resource.converters.CollectionProvider;
import com.devicehive.resource.converters.HiveEntityProvider;
import com.devicehive.resource.converters.JsonRawProvider;
import com.devicehive.resource.exceptions.AccessDeniedExceptionMapper;
import com.devicehive.resource.exceptions.ActionNotAllowedExceptionMapper;
import com.devicehive.resource.exceptions.AllExceptionMapper;
import com.devicehive.resource.exceptions.BadCredentialsExceptionMapper;
import com.devicehive.resource.exceptions.ConstraintViolationExceptionMapper;
import com.devicehive.resource.exceptions.HibernateConstraintViolationsMapper;
import com.devicehive.resource.exceptions.HiveExceptionMapper;
import com.devicehive.resource.exceptions.IllegalParametersExceptionMapper;
import com.devicehive.resource.exceptions.InvalidPrincipalExceptionMapper;
import com.devicehive.resource.exceptions.JsonParseExceptionMapper;
import com.devicehive.resource.exceptions.NoSuchElementExceptionMapper;
import com.devicehive.resource.exceptions.OptimisticLockExceptionMapper;
import com.devicehive.resource.exceptions.PersistenceExceptionMapper;
import com.devicehive.resource.filter.ReplacePostMethodFilter;
import com.devicehive.resource.impl.AuthApiInfoResourceImpl;
import com.devicehive.resource.impl.JwtTokenResourceImpl;
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
        registerClasses(AuthApiInfoResourceImpl.class,
                JwtTokenResourceImpl.class);

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        register(RequestContextFilter.class);
        register(LoggingFeature.class);
        register(ContentTypeFilter.class);

        register(CollectionProvider.class);
        register(HiveEntityProvider.class);
        register(JsonRawProvider.class);
        register(ReplacePostMethodFilter.class);

        registerClasses(
                AccessDeniedExceptionMapper.class,
                ActionNotAllowedExceptionMapper.class,
                AllExceptionMapper.class,
                BadCredentialsExceptionMapper.class,
                ConstraintViolationExceptionMapper.class,
                HibernateConstraintViolationsMapper.class,
                HiveExceptionMapper.class,
                IllegalParametersExceptionMapper.class,
                InvalidPrincipalExceptionMapper.class,
                JsonParseExceptionMapper.class,
                NoSuchElementExceptionMapper.class,
                OptimisticLockExceptionMapper.class,
                PersistenceExceptionMapper.class
        );

        register(ApiListingResource.class);
        register(SwaggerSerializers.class);
    }
}
