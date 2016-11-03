package com.devicehive.resource.impl;

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


import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.resource.ConfigurationResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

@Service
public class ConfigurationResourceImpl implements ConfigurationResource {

    @Autowired
    private ConfigurationService configurationService;

    @Value("${server.context-path}")
    private String contextPath;

    @Override
    public Response get(String name) {
        return Response.ok().entity(configurationService.get(name)).build();
    }

    @Override
    public Response setProperty(String name, String value) {
        configurationService.save(name, value);
        return Response.ok().build();
    }

    @Override
    public Response setPropertyGet(String name, String value) {
        configurationService.save(name, value);
        return Response.ok().build();
    }

    @Override
    public Response deleteProperty(String name) {
        configurationService.delete(name);
        return Response.noContent().build();
    }

}
