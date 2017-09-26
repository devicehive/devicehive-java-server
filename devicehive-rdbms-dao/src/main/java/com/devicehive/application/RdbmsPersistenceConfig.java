package com.devicehive.application;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.*;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.CollectionUtils;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.sql.DataSource;
import java.util.List;
import java.util.Properties;

@Configuration
@PropertySource("classpath:application-persistence.properties")
@EnableAutoConfiguration(exclude = { JacksonAutoConfiguration.class})
@EnableTransactionManagement(proxyTargetClass = true)
@EntityScan(basePackages = {"com.devicehive.model"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class RdbmsPersistenceConfig {
    
    @Value("${spring.jpa.properties.hibernate.cache.hazelcast.use_native_client:true}")
    private boolean useNativeClient;
    @Value("${hazelcast.group.name}")
    private String groupName;
    @Value("${hazelcast.group.password}")
    private String groupPassword;
    @Value("#{'${hazelcast.cluster.members}'.split(',')}")
    private List<String> clusterMembers;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JpaProperties properties;

    @Autowired
    private JpaVendorAdapter jpaVendorAdapter;

    @Bean
    @DependsOn(value = {"simpleApplicationContextHolder"})
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        final LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setSharedCacheMode(SharedCacheMode.ENABLE_SELECTIVE);
        factoryBean.setValidationMode(ValidationMode.CALLBACK);
        factoryBean.setJpaVendorAdapter(jpaVendorAdapter);
        factoryBean.setPackagesToScan("com.devicehive.model");

        final Properties props = new Properties();
        props.putAll(this.properties.getHibernateProperties(this.dataSource));
        if (useNativeClient) {
            props.put("hibernate.cache.hazelcast.native_client_group", groupName);
            props.put("hibernate.cache.hazelcast.native_client_password", groupPassword);
            if (!CollectionUtils.isEmpty(clusterMembers)) {
                props.put("hibernate.cache.hazelcast.native_client_address", clusterMembers.get(0));
            }
        }

        factoryBean.setJpaProperties(props);
        return factoryBean;
    }
}