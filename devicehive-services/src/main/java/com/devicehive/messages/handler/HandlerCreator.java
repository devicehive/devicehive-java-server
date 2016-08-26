package com.devicehive.messages.handler;


import java.util.UUID;

@Deprecated
public interface HandlerCreator<T> {

    Runnable getHandler(T message, UUID subId);
}
