package com.devicehive.proxy;

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

import com.devicehive.proxy.api.NotificationHandler;
import com.devicehive.proxy.api.ProxyClient;
import com.devicehive.shim.api.Response;
import com.devicehive.api.RequestResponseMatcher;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ProxyResponseHandler implements NotificationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProxyResponseHandler.class);

    private final Gson gson;
    private final RequestResponseMatcher requestResponseMatcher;

    @Autowired
    public ProxyResponseHandler(Gson gson, RequestResponseMatcher requestResponseMatcher) {
        this.gson = gson;
        this.requestResponseMatcher = requestResponseMatcher;
    }

    @Override
    public void handle(String message, ProxyClient client) {
        logger.debug("Received message from proxy client: " + message);
        final Response response = gson.fromJson(message, Response.class);

        requestResponseMatcher.offerResponse(response);
    }
}
