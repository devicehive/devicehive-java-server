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


import com.devicehive.resource.BaseApiInfoResource;
import com.devicehive.resource.PluginApiInfoResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Provide API information
 */
@Service
public class PluginApiInfoResourceImpl implements PluginApiInfoResource {

    private final BaseApiInfoResource baseApiInfoResource;

    @Autowired
    public PluginApiInfoResourceImpl(BaseApiInfoResource baseApiInfoResource) {
        this.baseApiInfoResource = baseApiInfoResource;
    }

    @Override
    public Response getApiInfo(UriInfo uriInfo, String protocol) {
        return baseApiInfoResource.getApiInfo(uriInfo, protocol);
    }
}

