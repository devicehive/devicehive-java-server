package com.devicehive.messages.bus.redis;

public interface Transformer<T, U> {
    U apply(T src);
}
