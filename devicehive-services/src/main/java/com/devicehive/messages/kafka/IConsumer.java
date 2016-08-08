package com.devicehive.messages.kafka;

/**
 * Author: Yuliia Vovk
 * Date: 25.02.16
 * Time: 18:38
 */
public interface IConsumer<T> {

    void submitMessage(T message);
}
