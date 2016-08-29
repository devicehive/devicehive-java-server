package com.devicehive.application;

import com.devicehive.handler.EchoRequestHandler;
import com.devicehive.handler.command.CommandInsertHandler;
import com.devicehive.handler.NotificationSubscribeRequestHandler;
import com.devicehive.handler.command.CommandSearchHandler;
import com.devicehive.handler.notification.NotificationInsertHandler;
import com.devicehive.handler.notification.NotificationSearchHandler;
import com.devicehive.model.rpc.Action;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

@Configuration
@ComponentScan("com.devicehive.handler")
public class RequestHandlersConfig {

    @Autowired
    private ApplicationContext context;

    @PostConstruct
    public void init() {
        requestHandlerMap().values().forEach(x -> context.getAutowireCapableBeanFactory().autowireBean(x));
    }

    @Bean
    public Map<Action, RequestHandler> requestHandlerMap() {
        return new HashMap<Action, RequestHandler>() {{
            put(Action.ECHO_REQUEST, new EchoRequestHandler());
            put(Action.NOTIFICATION_SEARCH_REQUEST, new NotificationSearchHandler());
            put(Action.NOTIFICATION_INSERT, new NotificationInsertHandler());
            put(Action.COMMAND_INSERT_REQUEST, new CommandInsertHandler());
            put(Action.NOTIFICATION_SUBSCRIBE_REQUEST, new NotificationSubscribeRequestHandler());
            put(Action.COMMAND_SEARCH_REQUEST, new CommandSearchHandler());
        }};
    }

}
