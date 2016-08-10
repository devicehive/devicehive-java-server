package com.devicehive.shim.kafka.client;

import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Properties;
import java.util.UUID;

@Configuration
public class KafkaRpcClientConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaRpcClientConfig.class);

    public static final String REQUEST_TOPIC = "request_topic";

    public static final String RESPONSE_TOPIC = "response_topic_" + UUID.randomUUID().toString();

    @Autowired
    private Environment env;

    @Value("${response.consumer.threads:1}")
    private int responseConsumerThreads;

    @Bean(destroyMethod = "shutdown")
    public RpcClient rpcClient() {
        Properties producerProps = producerProps();
        Properties consumerProps = consumerProps();

        return KafkaRpcClient.newBuilder()
                .withRequestProducerProps(producerProps)
                .withResponseConsumerProps(consumerProps)
                .withConsumerThreads(responseConsumerThreads)
                .withReplyTo(RESPONSE_TOPIC)
                .build();
    }

    private Properties producerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("bootstrap.servers"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, RequestSerializer.class.getName());
        return props;
    }

    private Properties consumerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("bootstrap.servers"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ResponseSerializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,  "response-group-" + UUID.randomUUID().toString());
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, env.getProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
        return props;
    }
}
