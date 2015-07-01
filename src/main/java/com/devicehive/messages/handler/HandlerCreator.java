package com.devicehive.messages.handler;


import java.util.UUID;

public interface HandlerCreator<T> {

    Runnable getHandler(T message, UUID subId);
}
