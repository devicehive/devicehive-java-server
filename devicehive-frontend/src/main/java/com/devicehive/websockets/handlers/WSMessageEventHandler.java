package com.devicehive.websockets.handlers;

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.websockets.WebSocketRequestProcessor;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.events.WSMessageEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import java.io.IOException;

@Component
public class WSMessageEventHandler implements EventHandler<WSMessageEvent> {
    private static final Logger logger = LoggerFactory.getLogger(WSMessageEventHandler.class);

    @Autowired
    private WebSocketRequestProcessor requestProcessor;

    @Override
    public void onEvent(WSMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        String response;
        try {
            response = execute(event.getRequest(), event.getSession()).toString();
        } catch (Exception ex) {
            logger.error("Unexpected exception occured during handling websocket message: {}", ex);
            response = JsonMessageBuilder
                    .createErrorResponseBuilder(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage())
                    .build().toString();
        }

        try {
            event.getSession().sendMessage(new TextMessage(response));
        } catch (IOException e) {
            logger.error("Exception handled during sending websocket message: {}", e);
            throw new RuntimeException(e);
        }
    }

    private JsonObject execute(JsonObject request, WebSocketSession session) {
        JsonObject response;
        try {
            response = requestProcessor.process(request, session).getResponseAsJson();
        } catch (BadCredentialsException ex) {
            logger.error("Unauthorized access", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials").build();
        } catch (AccessDeniedException ex) {
            logger.error("Access to action is denied", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized").build();
        } catch (HiveException ex) {
            logger.error("Error executing the request", ex);
            response = JsonMessageBuilder.createError(ex).build();
        } catch (ConstraintViolationException ex) {
            logger.error("Error executing the request", ex);
            response =
                    JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage())
                            .build();
        } catch (org.hibernate.exception.ConstraintViolationException ex) {
            logger.error("Error executing the request", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_CONFLICT, ex.getMessage())
                    .build();
        } catch (JsonParseException ex) {
            logger.error("Error executing the request", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_BAD_REQUEST,
                    Messages.INVALID_REQUEST_PARAMETERS).build();
        } catch (OptimisticLockException ex) {
            logger.error("Error executing the request", ex);
            logger.error("Data conflict", ex);
            response = JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_CONFLICT,
                    Messages.CONFLICT_MESSAGE).build();
        } catch (PersistenceException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                response =
                        JsonMessageBuilder.createErrorResponseBuilder(HttpServletResponse.SC_CONFLICT, ex.getMessage())
                                .build();
            } else {
                response = JsonMessageBuilder
                        .createErrorResponseBuilder(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage())
                        .build();
            }
        } catch (Exception ex) {
            logger.error("Error executing the request", ex);
            response = JsonMessageBuilder
                    .createErrorResponseBuilder(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage()).build();
        }

        return new JsonMessageBuilder()
                .addAction(request.get(JsonMessageBuilder.ACTION))
                .addRequestId(request.get(JsonMessageBuilder.REQUEST_ID))
                .include(response)
                .build();
    }
}
