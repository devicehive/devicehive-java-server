package com.devicehive.service.helpers;

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

import com.devicehive.model.rpc.ErrorResponse;
import com.devicehive.service.exception.BackendException;
import com.devicehive.shim.api.Response;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ResponseConsumer implements Consumer<Response> {

    private CompletableFuture<Response> future;

    public ResponseConsumer(CompletableFuture<Response> future) {
        this.future = future;
    }

    @Override
    public void accept(Response response) {
        if (response.isFailed()) {
            String message = response.getBody() != null
                    ? response.getBody().cast(ErrorResponse.class).getMessage()
                    : "Unexpected error occurred.";
            future.completeExceptionally(new BackendException(message, response.getErrorCode()));
        } else {
            future.complete(response);
        }
    }
}
