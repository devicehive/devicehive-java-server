package com.devicehive.shim.kafka.server;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientRequestDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(ClientRequestDispatcher.class);

    private RequestHandler requestHandler;
    private ExecutorService requestExecutor;
    private Producer<String, Response> responseProducer;
    private Gson gson;

    public ClientRequestDispatcher(RequestHandler requestHandler, ExecutorService requestExecutor, Producer<String, Response> responseProducer) {
        this.requestHandler = requestHandler;
        this.requestExecutor = requestExecutor;
        this.responseProducer = responseProducer;

        this.gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    void onRequestReceived(Request request) {
        final String replyTo = request.getReplyTo();
        assert replyTo != null;

        CompletableFuture.supplyAsync(() -> requestHandler.handle(request), requestExecutor)
                .handleAsync((ok, ex) -> {
                    if (ex != null) {
                        //todo better exception handling here
                        Response response = Response.newBuilder()
                                .withContentType(request.getContentType())
                                .withErrorCode(500)
                                .withLast(request.isSingleReplyExpected())
                                .withCorrelationId(request.getCorrelationId())
                                .buildFailed();
                        sendReply(replyTo, response);
                    } else {
                        sendReply(replyTo, ok);
                    }
                    return null;
                }, requestExecutor);

    }

    private void sendReply(String replyTo, Response response) {
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
