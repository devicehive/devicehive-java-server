package com.devicehive.client.context;


import com.devicehive.client.json.GsonFactory;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.devicehive.client.model.exceptions.HiveServerException;
import com.devicehive.client.model.exceptions.InternalHiveClientException;
import com.devicehive.client.websocket.HiveClientEndpoint;
import com.devicehive.client.websocket.HiveWebsocketHandler;
import com.devicehive.client.websocket.SimpleWebsocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;

import javax.ws.rs.core.Response;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static javax.ws.rs.core.Response.Status.Family;

public class HiveWebSocketClient implements Closeable {

    private static final String REQUEST_ID_MEMBER = "requestId";
    private static final Integer POOL_SIZE = 100;
    private static Logger logger = Logger.getLogger(HiveWebSocketClient.class);
    private final URI socket;
    private final HiveContext hiveContext;
    private HiveClientEndpoint endpoint;
    //    private ClientManager client;
    private Map<String, JsonObject> websocketResponsesMap = new HashMap<>();
    private ExecutorService pool = Executors.newFixedThreadPool(100);

    public HiveWebSocketClient(URI socket, HiveContext hiveContext) {
        this.socket = socket;
        this.hiveContext = hiveContext;
        endpoint = new HiveClientEndpoint(socket);
        endpoint.addMessageHandler(new HiveWebsocketHandler(hiveContext, websocketResponsesMap));
    }

    public void sendMessage(JsonObject message) {
        endpoint.sendMessage(message.toString());
        String requestId = message.get(REQUEST_ID_MEMBER).getAsString();
        websocketResponsesMap.put(requestId, null);
        processResponse(requestId);
    }

    public <T> T sendMessage(JsonObject message, Type typeOfResponse) {
        endpoint.sendMessage(message.toString());
        String requestId = message.get(REQUEST_ID_MEMBER).getAsString();
        websocketResponsesMap.put(requestId, null);
        return processResponse(typeOfResponse);
    }

    private void processResponse(final String requestId) {
        Future<SimpleWebsocketResponse> futureResult = pool.submit(new Callable<SimpleWebsocketResponse>() {

            @Override
            public SimpleWebsocketResponse call() throws Exception {
                SimpleWebsocketResponse response = null;
                while (response == null && !Thread.currentThread().isInterrupted()) {
                    JsonObject result = websocketResponsesMap.get(requestId);
                    if (result != null) {
                        Gson gson = GsonFactory.createGson();
                        try {
                            response = gson.fromJson(result, SimpleWebsocketResponse.class);
                        } catch (JsonSyntaxException e) {
                            throw new InternalHiveClientException("Wrong type of response!");
                        }

                    }
                }
                websocketResponsesMap.remove(requestId);
                return response;
            }
        });

        try {
            SimpleWebsocketResponse response = futureResult.get(1L, TimeUnit.MINUTES);
            if (response.getStatus().equals("success")) {
                logger.debug("Request with id:" + requestId + "proceed successfully");
                return;
            }
            Family errorFamily = Response.Status.Family.familyOf(response.getCode());
            switch (errorFamily) {
                case SERVER_ERROR:
                    logger.warn("Request id: " + requestId + ". Error message:" + response.getError() + ". Status code:"
                            + response.getCode());
                    throw new HiveServerException(response.getError(), response.getCode());
                case CLIENT_ERROR:
                    logger.warn("Request id: " + requestId + ". Error message:" + response.getError() + ". Status code:"
                            + response.getCode());
                    throw new HiveClientException(response.getError(), response.getCode());
            }
        } catch (InterruptedException | TimeoutException e) {
            logger.warn("For request id: " + requestId + ". Error message: " + e.getMessage(), e);
        } catch (ExecutionException e) {
            logger.warn("For request id: " + requestId + ". Error message: " + e.getMessage(), e);
            throw new InternalHiveClientException(e.getMessage(), e);
        }
    }

    private <T> T processResponse(Type typeOfResponse) {
        return null;
    }

    @Override
    public void close() throws IOException {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(10, TimeUnit.SECONDS))
                    logger.warn("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            logger.warn(ie);
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
