package com.devicehive.shim.kafka.server;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.MessageDispatcher;
import com.devicehive.shim.api.server.RequestHandler;
import com.lmax.disruptor.EventHandler;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.Charset;

public class ServerEventHandler implements EventHandler<ServerEvent>, MessageDispatcher {

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
            response = requestHandler.handle(request);
        } catch (Exception e) {
            String body = e.getClass().getName() + ": " + e.getMessage();
            response = Response.newBuilder()
                    .withContentType(request.getContentType())
                    .withErrorCode(500)
                    .withBody(body.getBytes(Charset.forName("UTF-8")))
                    .withLast(request.isSingleReplyExpected())
                    .withCorrelationId(request.getCorrelationId())
                    .buildFailed();
        }
        send(replyTo, response);
    }

    @Override
    public void send(String replyTo, Response response) {
        responseProducer.send(new ProducerRecord<>(replyTo, response.getCorrelationId(), response));
    }
}
