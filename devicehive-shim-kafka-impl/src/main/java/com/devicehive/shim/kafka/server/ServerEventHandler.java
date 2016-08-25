package com.devicehive.shim.kafka.server;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.MessageDispatcher;
import com.devicehive.shim.api.server.RequestHandler;
import com.lmax.disruptor.EventHandler;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ServerEventHandler implements EventHandler<ServerEvent>, MessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(ServerEventHandler.class);

    private RequestHandler requestHandler;
    private Producer<String, Response> responseProducer;

    public ServerEventHandler(RequestHandler requestHandler, Producer<String, Response> responseProducer) {
        this.requestHandler = requestHandler;
        this.responseProducer = responseProducer;
    }

    @Override
    public void onEvent(ServerEvent serverEvent, long sequence, boolean endOfBatch) throws Exception {
        final Request request = serverEvent.get();
        final String replyTo = request.getReplyTo();

        Response response;
        try {
            response = Optional.ofNullable(requestHandler.handle(request))
                    .orElseThrow(() -> new NullPointerException("Response must not be null"));
        } catch (Exception e) {
            logger.error("Unexpected exception occurred during request handling (action='{}', handler='{}')",
                    request.getBody().getAction(), requestHandler.getClass().getCanonicalName(), e);

            response = Response.newBuilder()
                    .withErrorCode(500)
                    .withLast(request.isSingleReplyExpected())
                    .buildFailed();
        }
        // set correlationId explicitly to prevent missing it in request
        response.setCorrelationId(request.getCorrelationId());
        send(replyTo, response);
    }

    @Override
    public void send(String replyTo, Response response) {
        responseProducer.send(new ProducerRecord<>(replyTo, response.getCorrelationId(), response));
    }
}
