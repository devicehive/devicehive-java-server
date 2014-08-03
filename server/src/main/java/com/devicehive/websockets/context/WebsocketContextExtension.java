package com.devicehive.websockets.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;

/**
 * Created by stas on 27.07.14.
 */
public class WebsocketContextExtension implements Extension {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketContextExtension.class);

    private WebsocketMessageContext websocketMessageContext = new WebsocketMessageContext();

    public WebsocketMessageContext getWebsocketMessageContext() {
        return websocketMessageContext;
    }


    public void createContext(@Observes AfterBeanDiscovery afterBeanDiscovery) {
        afterBeanDiscovery.addContext(websocketMessageContext);
    }
}
