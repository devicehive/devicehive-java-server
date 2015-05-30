package com.devicehive.application.async;

import com.devicehive.application.DeviceHiveApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jndi.JndiObjectFactoryBean;

import java.util.concurrent.ExecutorService;

@Configuration
@Profile({"jee-container", "!default", "!test"})
public class ContainerManagedAsyncConfig {

    @Bean(name = DeviceHiveApplication.WAIT_EXECUTOR)
    public ExecutorService jndiWaitExecutorService() {
        JndiObjectFactoryBean jndiObject = new JndiObjectFactoryBean();
        jndiObject.setJndiName("concurrent/DeviceHiveWaitService");
        jndiObject.setExpectedType(ExecutorService.class);
        jndiObject.setProxyInterface(ExecutorService.class);
        jndiObject.setResourceRef(true);
        return (ExecutorService) jndiObject.getObject();
    }

    @Bean(name = DeviceHiveApplication.MESSAGE_EXECUTOR)
    public ExecutorService jndiMessageExecutorService() {
        JndiObjectFactoryBean jndiObject = new JndiObjectFactoryBean();
        jndiObject.setJndiName("concurrent/DeviceHiveMessageService");
        jndiObject.setExpectedType(ExecutorService.class);
        jndiObject.setProxyInterface(ExecutorService.class);
        jndiObject.setResourceRef(true);
        return (ExecutorService) jndiObject.getObject();
    }

}
