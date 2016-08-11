package com.devicehive.shim.kafka.client.conf;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.kafka.client.KafkaRpcClient;
import com.devicehive.shim.kafka.client.RequestResponseMatcher;
import com.devicehive.shim.kafka.client.ResponseConsumerWorker;
import com.devicehive.shim.kafka.client.ServerResponseListener;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import com.devicehive.shim.kafka.server.conf.KafkaRpcServerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
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

import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
public class KafkaRpcClientConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaRpcClientConfig.class);

    public static final String RESPONSE_TOPIC = "response_topic_" + UUID.randomUUID().toString();

    @Autowired
    private Environment env;

    @Value("${response.consumer.threads:1}")
    private int responseConsumerThreads;

    @Bean
    public RequestResponseMatcher requestResponseMatcher() {
        return new RequestResponseMatcher();
    }

    @Bean
    public Producer<String, Request> kafkaRequestProducer() {
        return new KafkaProducer<>(producerProps());
    }

    @Bean(destroyMethod = "shutdown")
    public RpcClient rpcClient(Producer<String, Request> requestProducer, RequestResponseMatcher responseMatcher,
                               ServerResponseListener responseListener) {
        return new KafkaRpcClient(KafkaRpcServerConfig.REQUEST_TOPIC, RESPONSE_TOPIC, requestProducer, responseMatcher, responseListener);
    }

    @Bean
    public ServerResponseListener serverResponseListener(RequestResponseMatcher responseMatcher) {
        ExecutorService executor = Executors.newFixedThreadPool(responseConsumerThreads);
        Properties consumerProps = consumerProps();
        ServerResponseListener listener = new ServerResponseListener(RESPONSE_TOPIC, responseMatcher, consumerProps, executor);
        listener.startWorkers(responseConsumerThreads);
        return listener;
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
