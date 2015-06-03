package com.devicehive.application.websocket;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.websockets.ClientWebSocketHandler;
import com.devicehive.websockets.DeviceWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import javax.servlet.ServletContext;

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
     * Refer to <a href="http://docs.spring.io/spring/docs/current/spring-framework-reference/html/websocket.html#websocket-server-runtime-configuration">Configuring the WebSocket Engine</a>
     * (tomcat, glassfish, wildfly)
     *
     * Above link states that this is working in glassfish, but this bean causes following exception:
     *
     *  Caused by: java.lang.IllegalStateException: A ServletContext is required to access the javax.websocket.server.ServerContainer instance
     *       at org.springframework.util.Assert.state(Assert.java:385) ~[spring-core-4.1.6.RELEASE.jar:4.1.6.RELEASE]
     *       at org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean.afterPropertiesSet(ServletServerContainerFactoryBean.java:99) ~[spring-websocket-4.1.6.RELEASE.jar:4.1.6.RELEASE]
     *       at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.invokeInitMethods(AbstractAutowireCapableBeanFactory.java:1633) ~[spring-beans-4.1.6.RELEASE.jar:4.1.6.RELEASE]
     *       at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1570) ~[spring-beans-4.1.6.RELEASE.jar:4.1.6.RELEASE]
     *       ... 45 common frames omitted
     *
     * TODO: make profile specific
     */
    @Profile({"!jee-container"})
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
