package com.devicehive.service.helpers;

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
            String message = ((ErrorResponse) response.getBody()).getMessage();
            future.completeExceptionally(new BackendException(message, response.getErrorCode()));
        } else {
            future.complete(response);
        }
    }
}
