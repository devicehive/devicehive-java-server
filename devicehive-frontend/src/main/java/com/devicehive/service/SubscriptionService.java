package com.devicehive.service;

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

import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.rpc.ListSubscribeRequest;
import com.devicehive.model.rpc.ListSubscribeResponse;
import com.devicehive.shim.api.Action;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Component
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    private final RpcClient rpcClient;

    @Autowired
    public SubscriptionService(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public Map<Long, Filter> list(Set<Long> subscriptionIds) {
        ListSubscribeRequest listSubscribeRequest = new ListSubscribeRequest(subscriptionIds);
        Request request = Request.newBuilder()
                .withBody(listSubscribeRequest)
                .build();

        CompletableFuture<Map<Long, Filter>> future = new CompletableFuture<>();
        Consumer<Response> responseConsumer = response -> {
            Action resAction = response.getBody().getAction();
            if (resAction.equals(Action.LIST_SUBSCRIBE_RESPONSE)) {
                future.complete(response.getBody().cast(ListSubscribeResponse.class).getSubscriptions());
            } else {
                logger.warn("Unknown action received from backend {}", resAction);
            }
        };

        rpcClient.call(request, responseConsumer);
        return future.join();
    }
}
