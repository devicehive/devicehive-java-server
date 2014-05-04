package com.devicehive.client;


public interface MessageHandler<M> {

    void handle(M message);
}
