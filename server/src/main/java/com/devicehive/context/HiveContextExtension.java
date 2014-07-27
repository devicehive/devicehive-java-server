package com.devicehive.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

/**
 * Created by stas on 27.07.14.
 */
public class HiveContextExtension implements Extension {

    private static final Logger logger = LoggerFactory.getLogger(HiveContextExtension.class);

    public void createContext(@Observes AfterBeanDiscovery afterBeanDiscovery) {
        afterBeanDiscovery.addContext(new HiveRequestContext());
    }
}
