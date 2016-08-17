package com.devicehive.shim.config.server;

import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.shim.api.server.RpcServer;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import com.devicehive.shim.kafka.server.KafkaMessageDispatcher;
import com.devicehive.shim.kafka.server.KafkaRpcServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Bean
    public Producer<String, Response> kafkaResponseProducer() {
        return new KafkaProducer<>(producerProps());
    }

    @Bean
    public KafkaMessageDispatcher clientRequestHandler(RequestHandler requestHandler, Producer<String, Response> responseProducer) {
        ExecutorService workerExecutor = Executors.newFixedThreadPool(workerThreads);
        return new KafkaMessageDispatcher(requestHandler, workerExecutor, responseProducer);
    }

    @Bean(destroyMethod = "shutdown")
    public RpcServer rpcServer(KafkaMessageDispatcher messageDispatcher) {
        Properties consumerProps = consumerProps();
        ExecutorService consumerExecutor = Executors.newFixedThreadPool(consumerThreads);
        RpcServer rpcServer = new KafkaRpcServer(REQUEST_TOPIC, consumerThreads, consumerProps, consumerExecutor, messageDispatcher);
        rpcServer.start();
        return rpcServer;
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
