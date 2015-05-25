package com.devicehive.application.websocket;

import com.devicehive.websockets.HiveClientEndpoint;
import com.devicehive.websockets.HiveDeviceEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

//@Configuration
public class WebSocketConfig {

    @Bean
    public HiveDeviceEndpoint deviceEndpoint() {
        return new HiveDeviceEndpoint();
    }

    @Bean
    public HiveClientEndpoint clientEndpoint() {
        return new HiveClientEndpoint();
    }

    @Bean
    public ServerEndpointExporter endpointExporter() {
        return new ServerEndpointExporter();
    }

}
