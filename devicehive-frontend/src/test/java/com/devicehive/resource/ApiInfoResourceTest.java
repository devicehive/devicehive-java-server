package com.devicehive.resource;

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

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.configuration.Constants;
import com.devicehive.vo.ApiInfoVO;
import com.devicehive.vo.ClusterConfigVO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static java.util.Collections.emptyMap;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ApiInfoResourceTest extends AbstractResourceTest {

    @Autowired
    private Environment env;

    @Test
    public void should_return_API_info() throws Exception {
        ApiInfoVO apiInfo = performRequest("info", "GET", emptyMap(), emptyMap(), null, OK, ApiInfoVO.class);
        assertThat(apiInfo.getServerTimestamp(), notNullValue());
        assertThat(apiInfo.getRestServerUrl(), nullValue());
        assertThat(apiInfo.getWebSocketServerUrl(), is(wsBaseUri() + "/websocket"));
    }

    @Test
    public void should_return_cluster_config() throws Exception {
        ClusterConfigVO clusterConfig = performRequest("info/config/cluster", "GET", emptyMap(), emptyMap(), null, OK, ClusterConfigVO.class);
        assertThat(clusterConfig, notNullValue());
        assertThat(clusterConfig.getBootstrapServers(), is(env.getProperty(Constants.BOOTSTRAP_SERVERS)));
        assertThat(clusterConfig.getZookeeperConnect(), is(env.getProperty(Constants.ZOOKEEPER_CONNECT)));
        assertThat(clusterConfig.getThreadsCount(), is(1));
    }
}
