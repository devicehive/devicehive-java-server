package com.devicehive.shim.api.server;

public interface RpcServer {

    void addListener(Listener listener);

    void start();

    void shutdown();

}
