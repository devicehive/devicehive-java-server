package com.devicehive.shim;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.kafka.client.KafkaRpcClient;
import com.devicehive.shim.kafka.client.ResponseConsumerWorker;
import com.devicehive.shim.kafka.client.ResponseSupplier;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class KafkaRpcConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaRpcConfig.class);

    public static final String REQUEST_TOPIC = "request_topic";

    public static final String RESPONSE_TOPIC = "response_topic_";

    @Autowired
    private Environment env;

    @Value("${response.consumer.threads:1}")
    private int responseConsumerThreads;

    @Bean
    public RpcClient rpcClient(Producer<String, Request> requestProducer, ResponseSupplier responseSupplier) {
        return new KafkaRpcClient(requestProducer, responseSupplier);
    }

    @Bean
    public ResponseSupplier responseSupplier() {
        return new ResponseSupplier();
    }

    @Bean(name = "responseExecutor", destroyMethod = "shutdown")
    public ExecutorService responseConsumerExecutor() {
        return Executors.newFixedThreadPool(responseConsumerThreads);
    }

    @Bean
    @Lazy(false)
    public List<ResponseConsumerWorker> consumerWorkers(ResponseSupplier responseSupplier,
                                                        @Qualifier("responseExecutor") ExecutorService executor) {
        String responseTopic = RESPONSE_TOPIC + UUID.randomUUID().toString();
        Properties props = consumerProps();
        List<ResponseConsumerWorker> workers = new ArrayList<>(responseConsumerThreads);
        for (int i = 0; i < responseConsumerThreads; i++) {
            KafkaConsumer<String, Response> consumer = new KafkaConsumer<>(props);
            ResponseConsumerWorker worker = new ResponseConsumerWorker(responseTopic, responseSupplier, consumer);
            workers.add(worker);
            executor.submit(worker);
        }
        addConsumersShutdownHook(workers);
        return workers;
    }

    @Bean
    public Producer<String, Request> requestProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("bootstrap.servers"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, RequestSerializer.class.getName());

        logger.info("Creating Kafka request producer");
        return new KafkaProducer<>(props);
    }

    private void addConsumersShutdownHook(List<ResponseConsumerWorker> workers) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                workers.forEach(ResponseConsumerWorker::shutdown);
            }
        });
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
