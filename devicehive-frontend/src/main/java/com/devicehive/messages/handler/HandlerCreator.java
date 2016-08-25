package com.devicehive.messages.handler;

public interface HandlerCreator<T> {

    Runnable getHandler(T message, String subId);
}
