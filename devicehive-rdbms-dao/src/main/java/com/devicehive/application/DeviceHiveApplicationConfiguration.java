package com.devicehive.application;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = { JacksonAutoConfiguration.class})
@EnableTransactionManagement(proxyTargetClass = true)
@EntityScan(basePackages = {"com.devicehive.model"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class DeviceHiveApplicationConfiguration {
}
