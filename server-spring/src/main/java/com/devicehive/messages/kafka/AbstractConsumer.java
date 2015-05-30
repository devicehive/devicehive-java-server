package com.devicehive.messages.kafka;

import com.devicehive.application.DeviceHiveApplication;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

/**
 * Created by tmatvienko on 1/29/15.
 */
public abstract class AbstractConsumer<T> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractConsumer.class);

    @Async(DeviceHiveApplication.MESSAGE_EXECUTOR)
    public void subscribe(KafkaStream a_stream, int a_threadNumber) {
        logger.info("{}: Kafka consumer started... {} ", Thread.currentThread().getName(), a_threadNumber);
        ConsumerIterator<String, T> it = a_stream.iterator();
        while (it.hasNext()) {
            T message = it.next().message();
            logger.debug("Message arrived -> 'thread_name': {}, 'thread_number': {}, 'message': {}", Thread.currentThread().getName(), a_threadNumber, message);
            submitMessage(message);
        }
        logger.info("Shutting down Thread: " + a_threadNumber);
    }

    public abstract void submitMessage(T message);
}
