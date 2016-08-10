package com.devicehive.shim.kafka;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.api.server.Listener;
import com.devicehive.shim.api.server.RpcServer;
import com.devicehive.shim.kafka.client.KafkaRpcClient;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import com.devicehive.shim.kafka.server.KafkaRpcServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.nio.charset.Charset;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class KafkaRpcTest {

    public static void main(String ... args) throws InterruptedException, ExecutionException, TimeoutException {
        RpcServer server = createServer();
        server.start();

        RpcClient client = createClient();

        Request request = Request.newBuilder()
                .withContentType("text/plain")
                .withBody("Johnny".getBytes(Charset.forName("UTF-8")))
                .withCorrelationId(UUID.randomUUID().toString())
                .build();

        CompletableFuture<Response> future = client.call(request);
        Response response = future.get(20, TimeUnit.SECONDS);
        System.out.println("Responded -> " + new String(response.getBody()));

        client.shutdown();
        server.shutdown();

        System.exit(0);
    }

    public static RpcServer createServer() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ResponseSerializer.class.getName());

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, RequestSerializer.class.getName());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG,  "request-group");
        consumerProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        Listener listener = request -> "Hello " + new String(request.getBody());

       return KafkaRpcServer.newBuilder()
                .withConsumerThreads(1)
                .withWorkerThreads(1)
                .withListener(listener)
                .withRequestConsumerProps(consumerProps)
                .withResponseProducerProps(producerProps)
                .build();
    }

    public static RpcClient createClient() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, RequestSerializer.class.getName());

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ResponseSerializer.class.getName());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG,  "response-group-" + UUID.randomUUID().toString());
        consumerProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        return KafkaRpcClient.newBuilder()
                .withConsumerThreads(1)
                .withReplyTo("reply_topic")
                .withRequestProducerProps(producerProps)
                .withResponseConsumerProps(consumerProps)
                .build();
    }
}
