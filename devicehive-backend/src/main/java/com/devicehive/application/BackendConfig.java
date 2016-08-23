package com.devicehive.application;

import com.devicehive.json.GsonFactory;
import com.devicehive.model.rpc.EchoRequest;
import com.devicehive.model.rpc.EchoResponse;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackendConfig {

    @Bean
    public Gson gson() {
        return GsonFactory.createGson();
    }

    @Bean
    public RequestHandler requestHandler() {
        // FIXME: this handler was implemented just as a POC and must be removed later
        return request -> Response.newBuilder()
                .withBody(new EchoResponse(((EchoRequest) request.getBody()).getRequest())) // simple echo
                .withCorrelationId(request.getCorrelationId())
                .withLast(true)
                .buildSuccess();
    }
}
