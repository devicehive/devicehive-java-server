package com.devicehive.messages.kafka;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Asynchronous;

/**
 * Created by tmatvienko on 1/29/15.
 */
public abstract class AbstractConsumer<T> {
    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractConsumer.class);

    @Asynchronous
    public void subscribe(KafkaStream a_stream, int a_threadNumber) {
        LOGGER.info("{}: Kafka device notifications consumer started... {} ", Thread.currentThread().getName(), a_threadNumber);
        ConsumerIterator<String, T> it = a_stream.iterator();
        while (it.hasNext()) {
            T message = it.next().message();
            LOGGER.debug("{}: Thread {}: {}", Thread.currentThread().getName(), a_threadNumber, message);
            submitMessage(message);
        }
        LOGGER.info("Shutting down Thread: " + a_threadNumber);
    }

    public abstract void submitMessage(T message);
}
