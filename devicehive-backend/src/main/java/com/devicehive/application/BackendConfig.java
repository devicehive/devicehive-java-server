package com.devicehive.application;

import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackendConfig {

    @Bean
    public RequestHandler requestHandler() {
        // FIXME: this handler was implemented just as a POC and must be removed later
        return request -> Response.newBuilder()
                .withCorrelationId(request.getCorrelationId())
                .withBody(request.getBody()) // simple echo
                .withLast(true)
                .buildSuccess();
    }
}
