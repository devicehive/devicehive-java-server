package com.devicehive.application;

import com.devicehive.configuration.Constants;
import com.devicehive.websockets.ClientWebSocketHandler;
import com.devicehive.websockets.DeviceWebSocketHandler;
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

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry
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

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxBinaryMessageBufferSize(Constants.WEBSOCKET_MAX_BUFFER_SIZE);
        container.setMaxTextMessageBufferSize(Constants.WEBSOCKET_MAX_BUFFER_SIZE);
        //TODO: Implement with configurationService
//        container.setMaxSessionIdleTimeout(
//                configurationService.getLong(Constants.WEBSOCKET_SESSION_PING_TIMEOUT, Constants.WEBSOCKET_SESSION_PING_TIMEOUT_DEFAULT));
        return container;
    }

}
