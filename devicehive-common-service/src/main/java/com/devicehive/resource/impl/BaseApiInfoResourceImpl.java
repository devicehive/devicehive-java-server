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


import com.devicehive.configuration.Constants;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.resource.BaseApiInfoResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.ApiInfoVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Provide API information
 */
@Service
public class BaseApiInfoResourceImpl implements BaseApiInfoResource {

    private static final Logger logger = LoggerFactory.getLogger(BaseApiInfoResourceImpl.class);

    protected final TimestampService timestampService;
    
    @Value("${server.context-path}")
    protected String contextPath;

    @Value("${build.version}")
    protected String appVersion;

    @Autowired
    public BaseApiInfoResourceImpl(TimestampService timestampService) {
        this.timestampService = timestampService;
    }

    @Override
    public Response getApiInfo(UriInfo uriInfo, String protocol) {
        logger.debug("ApiInfoVO requested");
        ApiInfoVO apiInfo = new ApiInfoVO();
        String version = Constants.class.getPackage().getImplementationVersion();

        if(version == null) {
            apiInfo.setApiVersion(appVersion);
        } else {
            apiInfo.setApiVersion(version);
        }
        apiInfo.setServerTimestamp(timestampService.getDate());
        
        // Generate websocket url based on current request url
        int port = uriInfo.getBaseUri().getPort();
        String wsScheme = "https".equals(protocol) ? "wss" : "ws";
        if (port == -1) {
            apiInfo.setWebSocketServerUrl(wsScheme + "://" + uriInfo.getBaseUri().getHost() + contextPath + "/websocket");
        } else {
            apiInfo.setWebSocketServerUrl(wsScheme + "://" + uriInfo.getBaseUri().getHost() + ":" + uriInfo.getBaseUri().getPort() + contextPath + "/websocket");
        }
        
        return ResponseFactory.response(Response.Status.OK, apiInfo, JsonPolicyDef.Policy.REST_SERVER_INFO);
    }

}
