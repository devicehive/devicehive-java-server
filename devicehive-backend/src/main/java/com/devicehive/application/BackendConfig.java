package com.devicehive.application;

import com.devicehive.handler.EchoRequestHandler;
import com.devicehive.handler.RequestDispatcher;
import com.devicehive.json.GsonFactory;
import com.devicehive.model.rpc.Action;
import com.devicehive.shim.api.server.RequestHandler;
import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class BackendConfig {

    @Bean
    public Gson gson() {
        return GsonFactory.createGson();
    }

    @Bean
    public RequestHandler requestHandler() {
        return new RequestDispatcher(new HashMap<Action, RequestHandler>() {{
            put(Action.ECHO_REQUEST, new EchoRequestHandler());
        }});
    }
}
