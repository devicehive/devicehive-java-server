package com.devicehive.shim.config.client;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.config.server.KafkaRpcServerConfig;
import com.devicehive.shim.kafka.client.KafkaRpcClient;
import com.devicehive.shim.kafka.client.RequestResponseMatcher;
import com.devicehive.shim.kafka.client.ServerResponseListener;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import com.google.gson.Gson;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Profile("rpc-client")
@PropertySource("classpath:kafka.properties")
public class KafkaRpcClientConfig {

    public static String RESPONSE_TOPIC;

    static {
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            String prefix = Optional.ofNullable(ni)
                    .map(n -> {
                        try {
                            return n.getHardwareAddress();
                        } catch (SocketException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(mac -> Base64.getEncoder().encodeToString(mac)).orElse(UUID.randomUUID().toString());
            RESPONSE_TOPIC = "response_topic_" + prefix;
        } catch (SocketException | UnknownHostException e) {
            RESPONSE_TOPIC = "response_topic_" + UUID.randomUUID().toString();
        }
    }

    @Autowired
    private Environment env;

    @Value("${rpc.client.response-consumer.threads:1}")
    private int responseConsumerThreads;

    @Bean
    public RequestResponseMatcher requestResponseMatcher() {
        return new RequestResponseMatcher();
    }

    @Bean
    public Producer<String, Request> kafkaRequestProducer(Gson gson) {
        return new KafkaProducer<>(producerProps(), new StringSerializer(), new RequestSerializer(gson));
    }

    @Bean(destroyMethod = "shutdown")
    public RpcClient rpcClient(Producer<String, Request> requestProducer, RequestResponseMatcher responseMatcher,
                               ServerResponseListener responseListener) {
        return new KafkaRpcClient(KafkaRpcServerConfig.REQUEST_TOPIC, RESPONSE_TOPIC, requestProducer, responseMatcher, responseListener);
    }

    @Bean
    public ServerResponseListener serverResponseListener(RequestResponseMatcher responseMatcher, Gson gson) {
        ExecutorService executor = Executors.newFixedThreadPool(responseConsumerThreads);
        Properties consumerProps = consumerProps();
        ServerResponseListener listener = new ServerResponseListener(RESPONSE_TOPIC, responseConsumerThreads,
                responseMatcher, consumerProps, executor, new ResponseSerializer(gson));
        listener.startWorkers();
        return listener;
    }

    private Properties producerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("bootstrap.servers"));
        return props;
    }

    private Properties consumerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("bootstrap.servers"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG,  "response-group-" + UUID.randomUUID().toString());
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, env.getProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
        return props;
    }
}
