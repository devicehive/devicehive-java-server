package com.devicehive.client;


public interface HiveMessageHandler<M> {

    void handle(M message);
}
