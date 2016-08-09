package com.devicehive.shim.api.client;

import com.devicehive.shim.api.server.Response;

import java.util.concurrent.CompletableFuture;

public interface RpcClient {

    CompletableFuture<Response> call(Request request);

    void push(Request request);
    
}
