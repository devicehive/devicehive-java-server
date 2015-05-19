package com.devicehive.websockets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.server.ServerEndpointConfig;

public class HiveConfigurator extends ServerEndpointConfig.Configurator {

    private static final Logger logger = LoggerFactory.getLogger(HiveConfigurator.class);

    @Override
    public boolean checkOrigin(String originHeaderValue) {
        return true;
    }
}
