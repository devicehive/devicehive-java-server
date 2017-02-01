package com.devicehive.shim.kafka.fixture;

/*
 * #%L
 * DeviceHive Shim Kafka Implementation
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

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;

public class RequestHandlerWrapper implements RequestHandler {
    private RequestHandler delegate;

    @Override
    public Response handle(Request request) {
        if (delegate == null) {
            throw new IllegalStateException("Request handler wasn't initialized");
        }

        return delegate.handle(request);
    }

    public void setDelegate(RequestHandler delegate) {
        this.delegate = delegate;
    }
}
