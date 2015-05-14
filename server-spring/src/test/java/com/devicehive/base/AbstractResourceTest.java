package com.devicehive.base;

import com.devicehive.configuration.DeviceHiveApplication;
import com.devicehive.configuration.JerseyConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.ws.rs.core.Application;

public abstract class AbstractResourceTest extends JerseyTest {

    public AbstractResourceTest() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
    }

    @Override
    protected Application configure() {
        return new JerseyConfig()
                .property("contextConfig", new AnnotationConfigApplicationContext(DeviceHiveApplication.class));
    }

}
