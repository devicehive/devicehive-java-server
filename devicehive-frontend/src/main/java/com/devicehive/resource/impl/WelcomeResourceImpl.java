package com.devicehive.resource.impl;

/*
 * #%L
 * DeviceHive Frontend Logic
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

import com.devicehive.configuration.Constants;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.resource.WelcomeResource;
import com.devicehive.resource.util.ResponseFactory;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

@Service
public class WelcomeResourceImpl implements WelcomeResource {

    @Override
    public Response getWelcomeInfo() {
        return ResponseFactory.response(Response.Status.OK, Constants.WELCOME_MESSAGE,
                JsonPolicyDef.Policy.REST_SERVER_INFO);
    }
}

