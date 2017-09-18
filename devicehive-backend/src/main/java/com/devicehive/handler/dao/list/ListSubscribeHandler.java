package com.devicehive.handler.dao.list;

/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.eventbus.FilterRegistry;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.rpc.ListSubscribeRequest;
import com.devicehive.model.rpc.ListSubscribeResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ListSubscribeHandler implements RequestHandler {

    private FilterRegistry filterRegistry;

    @Autowired
    public void setFilterRegistry(FilterRegistry filterRegistry) {
        this.filterRegistry = filterRegistry;
    }

    @Override
    public Response handle(Request request) {
        final ListSubscribeRequest req = (ListSubscribeRequest) request.getBody();
        Map<Long, Filter> onResponse = new HashMap<>();

        req.getSubscriptionIds().forEach(subId -> onResponse.put(subId, filterRegistry.getFilter(subId)));

        return Response.newBuilder()
                .withBody(new ListSubscribeResponse(onResponse))
                .buildSuccess();
    }
}
