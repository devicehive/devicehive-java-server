package com.devicehive.application;

import com.devicehive.handler.EchoRequestHandler;
import com.devicehive.handler.notification.NotificationSearchHandler;
import com.devicehive.json.GsonFactory;
import com.devicehive.model.rpc.Action;
import com.devicehive.shim.api.server.RequestHandler;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ComponentScan("com.devicehive.handler")
public class BackendConfig {

    @PostConstruct
    public void init() {
        requestHandlerMap().values().forEach(x -> context.getAutowireCapableBeanFactory().autowireBean(x));
    }

    @Autowired
    private ApplicationContext context;

    @Bean
    public Gson gson() {
        return GsonFactory.createGson();
    }

    @Bean
    public Map<Action, RequestHandler> requestHandlerMap() {
        return new HashMap<Action, RequestHandler>() {{
            put(Action.ECHO_REQUEST, new EchoRequestHandler());
            put(Action.NOTIFICATION_SEARCH_REQUEST, new NotificationSearchHandler());
        }};
    }
}
