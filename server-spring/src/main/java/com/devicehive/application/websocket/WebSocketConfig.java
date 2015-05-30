package com.devicehive.application.websocket;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.websockets.ClientWebSocketHandler;
import com.devicehive.websockets.DeviceWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(deviceHandler(), "/websocket/device").setAllowedOrigins("*")
                .addHandler(clientHandler(), "/websocket/client").setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler deviceHandler() {
        return new DeviceWebSocketHandler();
    }

    @Bean
    public WebSocketHandler clientHandler() {
        return new ClientWebSocketHandler();
    }

    /**
     * For tomcat, glassfish and wildfly only
     * refer to <a href="http://docs.spring.io/spring/docs/current/spring-framework-reference/html/websocket.html#websocket-server-runtime-configuration">Configuring the WebSocket Engine</a>
     *
     * TODO: make profile specific
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(Constants.WEBSOCKET_MAX_BUFFER_SIZE);
        container.setMaxTextMessageBufferSize(Constants.WEBSOCKET_MAX_BUFFER_SIZE);
        container.setMaxSessionIdleTimeout(
                configurationService.getLong(Constants.WEBSOCKET_SESSION_PING_TIMEOUT, Constants.WEBSOCKET_SESSION_PING_TIMEOUT_DEFAULT));
        return container;
    }

}
