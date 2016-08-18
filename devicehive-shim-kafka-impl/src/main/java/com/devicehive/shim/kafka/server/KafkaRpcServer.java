package com.devicehive.shim.kafka.server;

import com.devicehive.shim.api.server.MessageDispatcher;
import com.devicehive.shim.api.server.RpcServer;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

public class KafkaRpcServer implements RpcServer {

    private Disruptor<ServerEvent> disruptor;
    private RequestConsumer requestConsumer;
    private ServerEventHandler eventHandler;

    public KafkaRpcServer(Disruptor<ServerEvent> disruptor, RequestConsumer requestConsumer, ServerEventHandler eventHandler) {
        this.disruptor = disruptor;
        this.requestConsumer = requestConsumer;
        this.eventHandler = eventHandler;
    }

    @Override
    public void start() {
        disruptor.handleEventsWith(eventHandler);
        disruptor.start();

        RingBuffer<ServerEvent> ringBuffer = disruptor.getRingBuffer();
        requestConsumer.startConsumers(ringBuffer);
    }

    @Override
    public void shutdown() {
        requestConsumer.shutdownConsumers();
        disruptor.shutdown();
    }

    @Override
    public MessageDispatcher getDispatcher() {
        return eventHandler;
    }
}
