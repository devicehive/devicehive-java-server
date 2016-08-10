package com.devicehive.shim.kafka.server;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.server.Listener;
import com.devicehive.shim.api.server.RpcServer;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Properties;
import java.util.UUID;

@Configuration
public class KafkaRpcServerConfig {

    public static final String SERVER_REQUEST_GROUP = "request-consumer-group";

    @Autowired
    private Environment env;

    @Value("${request.consumer.threads:1}")
    private int consumerThreads;

    @Value("${worker.threads:1}")
    private int workerThreads;

    @Bean(destroyMethod = "shutdown")
    public RpcServer rpcServer(Listener listener) {
        Properties consumerProps = consumerProps();
        Properties producerProps = producerProps();

        RpcServer server = KafkaRpcServer.newBuilder()
                .withConsumerThreads(consumerThreads)
                .withWorkerThreads(workerThreads)
                .withListener(listener)
                .withRequestConsumerProps(consumerProps)
                .withResponseProducerProps(producerProps)
                .build();
        server.start();
        return server;
    }

    @Bean
    public Listener listener() {
        return request -> "Hello " + request.getCorrelationId();
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
        props.put(ConsumerConfig.GROUP_ID_CONFIG,  SERVER_REQUEST_GROUP);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, env.getProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
        return props;
    }

}
