package com.devicehive.shim.api.client;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;

import java.util.function.Consumer;

public interface RpcClient {

    void call(Request request, Consumer<Response> callback);

    void push(Request request);

    default void start() { }

    default void shutdown() { }
}
