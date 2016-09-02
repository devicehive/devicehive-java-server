package com.devicehive.shim.kafka.server;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.RequestType;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.MessageDispatcher;
import com.devicehive.shim.api.server.RequestHandler;
import com.lmax.disruptor.EventHandler;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.devicehive.shim.api.RequestType.clientRequest;

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

        switch (request.getType()) {
            case clientRequest:
                logger.debug("Client request received {}", request);
                response = handleClientRequest(request);
                break;
            case ping:
                logger.info("Ping request received from {}", replyTo);
                response = Response.newBuilder().buildSuccess();
                break;
            default:
                logger.warn("Unknown type of request received {} from client with topic {}, correlationId = {}",
                        request.getType(), replyTo, request.getCorrelationId());
                response = Response.newBuilder()
                        .withErrorCode(404)
                        .buildFailed();
        }

        // set correlationId explicitly to prevent missing it in request
        response.setCorrelationId(request.getCorrelationId());
        send(replyTo, response);
    }

    private Response handleClientRequest(Request request) {
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
        return response;
    }

    @Override
    public void send(String replyTo, Response response) {
        responseProducer.send(new ProducerRecord<>(replyTo, response.getCorrelationId(), response));
    }
}
