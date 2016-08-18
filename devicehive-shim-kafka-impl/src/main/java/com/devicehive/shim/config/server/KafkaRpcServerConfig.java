package com.devicehive.shim.config.server;

import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.shim.api.server.RpcServer;
import com.devicehive.shim.kafka.server.KafkaRpcServer;
import com.devicehive.shim.kafka.server.RequestConsumer;
import com.devicehive.shim.kafka.server.ServerEvent;
import com.devicehive.shim.kafka.server.ServerEventHandler;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Profile("rpc-server")
@PropertySource("classpath:kafka.properties")
public class KafkaRpcServerConfig {

    public static final String REQUEST_TOPIC = "request_topic";

    @Autowired
    private Environment env;

    @Value("${server.request-consumer.group}")
    private String requestConsumerGroup;

    @Value("${server.request-consumer.threads:1}")
    private int consumerThreads;

    @Value("${server.worker.threads:1}")
    private int workerThreads;

    @Value("${lmax.buffer-size:1024}")
    private int bufferSize;

    @Value("${server.disruptor.wait-strategy}")
    private String waitStrategyType;

    @Bean(name = "server-producer")
    public Producer<String, Response> kafkaResponseProducer() {
        return new KafkaProducer<>(producerProps());
    }

    @Bean
    public ExecutorService workerExecutor() {
        return Executors.newFixedThreadPool(workerThreads);
    }

    @Bean
    public WaitStrategy disruptorWaitStrategy() {
        WaitStrategy strategy;
        switch (waitStrategyType) {
            case "sleeping":
                strategy = new SleepingWaitStrategy();
                break;
            case "yielding":
                strategy = new YieldingWaitStrategy();
                break;
            case "busy-spin":
                strategy = new BusySpinWaitStrategy();
            case "blocking":
            default:
                strategy = new BlockingWaitStrategy();
        }
        return strategy;
    }

    @Bean
    public Disruptor<ServerEvent> disruptor(@Qualifier("workerExecutor") ExecutorService workerExecutor, WaitStrategy waitStrategy) {
        ProducerType producerType = ProducerType.SINGLE;
        if (consumerThreads > 1) {
            producerType = ProducerType.MULTI;
        }

        return new Disruptor<>(ServerEvent::new, bufferSize,  workerExecutor, producerType, waitStrategy);
    }

    @Bean
    public ServerEventHandler serverEventHandler(RequestHandler requestHandler,
                                                 @Qualifier("server-producer") Producer<String, Response> responseProducer) {
        return new ServerEventHandler(requestHandler, responseProducer);
    }

    @Bean
    public RequestConsumer requestConsumer() {
        return new RequestConsumer(REQUEST_TOPIC, consumerProps(), consumerThreads);
    }

    @Bean
    public RpcServer rpcServer(Disruptor<ServerEvent> disruptor, RequestConsumer requestConsumer, ServerEventHandler eventHandler) {
        RpcServer server = new KafkaRpcServer(disruptor, requestConsumer, eventHandler);
        server.start();
        return server;
    }

    private Properties producerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("bootstrap.servers"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ResponseSerializer.class.getName());
        return props;
    }

    private Properties consumerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("bootstrap.servers"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, RequestSerializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,  requestConsumerGroup);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, env.getProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
        return props;
    }

}
