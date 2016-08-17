package com.devicehive.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.*;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@PropertySource("classpath:application-persistence.properties")
@EnableAutoConfiguration(exclude = { JacksonAutoConfiguration.class})
@EnableTransactionManagement(proxyTargetClass = true)
@EntityScan(basePackages = {"com.devicehive.model"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class RdbmsPersistenceConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JpaProperties properties;

    @Autowired
    private JpaVendorAdapter jpaVendorAdapter;

    @Bean
    @Autowired
    @DependsOn(value = {"hazelcast", "simpleApplicationContextHolder"})
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        final LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setSharedCacheMode(SharedCacheMode.ENABLE_SELECTIVE);
        factoryBean.setValidationMode(ValidationMode.CALLBACK);
        factoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        factoryBean.setPackagesToScan("com.devicehive.model");

        final Properties props = new Properties();
        props.putAll(this.properties.getHibernateProperties(this.dataSource));
        factoryBean.setJpaProperties(props);
        return factoryBean;
    }
}