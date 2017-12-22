package com.devicehive.resource.impl;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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


import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.query.PluginReqisterQuery;
import com.devicehive.model.query.PluginUpdateQuery;
import com.devicehive.model.updates.PluginUpdate;
import com.devicehive.resource.PluginResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.BaseDeviceService;
import com.devicehive.service.PluginRegisterService;
import com.devicehive.util.HiveValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import static com.devicehive.configuration.Messages.HEALTH_CHECK_FAILED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.PLUGIN_SUBMITTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

@Service
public class PluginResourceImpl implements PluginResource {

    private static final Logger logger = LoggerFactory.getLogger(PluginResourceImpl.class);

    private final HiveValidator hiveValidator;
    private final PluginRegisterService pluginRegisterService;

    @Autowired
    public PluginResourceImpl(HiveValidator hiveValidator, PluginRegisterService pluginRegisterService) {
        this.hiveValidator = hiveValidator;
        this.pluginRegisterService = pluginRegisterService;
    }
    
    @Override
    public void register(PluginReqisterQuery pluginReqisterQuery, PluginUpdate pluginUpdate, String authorization,
            @Suspended final AsyncResponse asyncResponse) {
        hiveValidator.validate(pluginUpdate);
        try {
            pluginRegisterService.register(pluginReqisterQuery, pluginUpdate, authorization)
                    .thenAccept(asyncResponse::resume
                    );
        } catch (ServiceUnavailableException e) {
            logger.warn(HEALTH_CHECK_FAILED);
            asyncResponse.resume(ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(), HEALTH_CHECK_FAILED)));
        }
    }

    @Override
    public void update(PluginUpdateQuery updateQuery, String authorization, AsyncResponse asyncResponse) {
        pluginRegisterService.update(updateQuery, authorization)
                .thenAccept(asyncResponse::resume);
    }

    @Override
    public void delete(String topicName, String authorization, AsyncResponse asyncResponse) {
        pluginRegisterService.delete(topicName, authorization)
                .thenAccept(asyncResponse::resume);
    }
}
