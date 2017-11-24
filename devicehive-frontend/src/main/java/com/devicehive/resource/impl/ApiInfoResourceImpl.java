package com.devicehive.resource.impl;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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


import com.devicehive.configuration.Constants;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.resource.ApiInfoResource;
import com.devicehive.resource.BaseApiInfoResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.CacheInfoVO;
import com.devicehive.vo.ClusterConfigVO;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Provide API information
 */
@Service
public class ApiInfoResourceImpl implements ApiInfoResource {

    private static final Logger logger = LoggerFactory.getLogger(ApiInfoResourceImpl.class);

    private final Environment env;
    private final LocalContainerEntityManagerFactoryBean entityManagerFactory;
    private final TimestampService timestampService;
    private final BaseApiInfoResource baseApiInfoResource;

    @Autowired
    public ApiInfoResourceImpl(TimestampService timestampService,
            Environment env,
            LocalContainerEntityManagerFactoryBean entityManagerFactory,
            BaseApiInfoResource baseApiInfoResource) {
        this.timestampService = timestampService;
        this.env = env;
        this.entityManagerFactory = entityManagerFactory;
        this.baseApiInfoResource = baseApiInfoResource;
    }

    @Override
    public Response getApiInfo(UriInfo uriInfo, String protocol) {
        return baseApiInfoResource.getApiInfo(uriInfo, protocol);
    }

    @Override
    public Response getApiInfoCache(UriInfo uriInfo) {
        logger.debug("ApiInfoVO requested");
        CacheInfoVO cacheInfoVO = new CacheInfoVO();
        cacheInfoVO.setServerTimestamp(timestampService.getDate());
        cacheInfoVO.setCacheStats(getCacheStats());

        return ResponseFactory.response(Response.Status.OK, cacheInfoVO, JsonPolicyDef.Policy.REST_SERVER_INFO);
    }

    @Override
    public Response getClusterConfig() {
        logger.debug("ClusterConfigVO requested");
        ClusterConfigVO clusterConfig = new ClusterConfigVO();
        clusterConfig.setBootstrapServers(env.getProperty(Constants.BOOTSTRAP_SERVERS));
        clusterConfig.setZookeeperConnect(env.getProperty(Constants.ZOOKEEPER_CONNECT));

        return ResponseFactory.response(Response.Status.OK, clusterConfig, JsonPolicyDef.Policy.REST_CLUSTER_CONFIG);
    }

    private String getCacheStats() {
        SessionFactory sessionFactory = entityManagerFactory.getNativeEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();

        return statistics.toString();
    }
}
