package com.devicehive.application;

import com.devicehive.json.GsonFactory;
import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.devicehive.eventbus.EventBus;
import com.devicehive.shim.api.server.RpcServer;

@Configuration
public class BackendConfig {

    @Bean
    public Gson gson() {
        return GsonFactory.createGson();
    }

    @Bean
    public EventBus eventBus(RpcServer rpcServer) {
        return new EventBus(rpcServer.getDispatcher());
    }
}
