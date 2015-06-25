package com.devicehive.application;

import com.devicehive.util.ApplicationContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;
import javax.validation.Validator;

@SpringBootApplication(exclude = { JacksonAutoConfiguration.class })
@ComponentScan("com.devicehive")
@EnableTransactionManagement(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EntityScan(basePackages = {"com.devicehive.model"})
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
public class DeviceHiveApplication extends SpringBootServletInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DeviceHiveApplication.class);

    public static final String WAIT_EXECUTOR = "DeviceHiveWaitService";
    public static final String MESSAGE_EXECUTOR = "DeviceHiveMessageService";

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment env;

    @PostConstruct
    public void initApp() {
        ApplicationContextHolder.getInstance().set(context);
    }

    public static void main(String ... args) {
        SpringApplication.run(DeviceHiveApplication.class);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(DeviceHiveApplication.class);
    }

    @Bean
    public Validator localValidator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean(destroyMethod = "destroy")
    public JedisPool jedisPool() {
        String host = env.getProperty("spring.redis.host");
        Integer port = Integer.parseInt(env.getProperty("spring.redis.port"));
        Integer timeout = Integer.parseInt(env.getProperty("spring.redis.timeout"));
        logger.info("Creating JedisPool {}:{}", host, port);
        return new JedisPool(new JedisPoolConfig(), host, port, timeout);
    }
}
