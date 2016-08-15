package com.devicehive.shim.kafka.server;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.MessageDispatcher;
import com.devicehive.shim.api.server.RequestHandler;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class KafkaMessageDispatcher implements MessageDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageDispatcher.class);

    private RequestHandler requestHandler;
    private ExecutorService requestExecutor;
    private Producer<String, Response> responseProducer;

    public KafkaMessageDispatcher(RequestHandler requestHandler, ExecutorService requestExecutor, Producer<String, Response> responseProducer) {
        this.requestHandler = requestHandler;
        this.requestExecutor = requestExecutor;
        this.responseProducer = responseProducer;
    }

    @Override
    public void onReceive(Request request) {
        final String replyTo = request.getReplyTo();
        assert replyTo != null;

        CompletableFuture.supplyAsync(() -> requestHandler.handle(request), requestExecutor)
                .handleAsync((ok, ex) -> {
                    Response response;
                    if (ex != null) {
                        String body = ex.getClass().getName() + ": " + ex.getMessage();
                        response = Response.newBuilder()
                                .withContentType(request.getContentType())
                                .withErrorCode(500)
                                .withBody(body.getBytes(Charset.forName("UTF-8")))
                                .withLast(request.isSingleReplyExpected())
                                .withCorrelationId(request.getCorrelationId())
                                .buildFailed();
                    } else {
                        response = ok;
                    }
                    send(replyTo, response);
                    return null;
                }, requestExecutor);

    }

    @Override
    public void send(String replyTo, Response response) {
        responseProducer.send(new ProducerRecord<>(replyTo, response.getCorrelationId(), response), (recordMetadata, e) -> {
            if (e != null) {
                logger.error("Send response failed", e);
            }
            logger.debug("Response sent successfully {}", response);
        });
    }

    public void shutdown() {
        if (requestExecutor != null) {
            requestExecutor.shutdown();
            try {
                requestExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.error("Exception occurred while shutting executor service: {}", e);
            }
        }
        if (responseProducer != null) {
            responseProducer.close();
        }
    }
}
