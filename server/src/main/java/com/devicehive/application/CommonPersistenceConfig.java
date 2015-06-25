package com.devicehive.application;

import com.devicehive.util.ApplicationContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class CommonPersistenceConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JpaProperties properties;

    @Autowired
    private JpaVendorAdapter jpaVendorAdapter;

    @Bean
    @Autowired
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(ApplicationContextHolder holder) {
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