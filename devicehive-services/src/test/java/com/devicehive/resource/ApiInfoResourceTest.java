package com.devicehive.resource;

import com.devicehive.application.JerseyConfig;
import com.devicehive.base.AbstractResourceTest;
import com.devicehive.configuration.Constants;
import com.devicehive.vo.ApiConfigVO;
import com.devicehive.vo.ApiInfoVO;
import com.devicehive.vo.ClusterConfigVO;
import com.devicehive.vo.IdentityProviderConfig;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
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

        //configure rest.url and websocket.url
        String path = String.format("configuration/%s/set", Constants.REST_SERVER_URL);
        performRequest(path, "GET", singletonMap("value", baseUri() + JerseyConfig.REST_PATH),
                singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), null, OK, Response.class);
        path = String.format("configuration/%s/set", Constants.WEBSOCKET_SERVER_URL);
        performRequest(path, "GET", singletonMap("value", wsBaseUri() + "/websocket"),
                singletonMap(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS)), null, OK, Response.class);

        apiInfo = performRequest("info", "GET", emptyMap(), emptyMap(), null, OK, ApiInfoVO.class);
        assertThat(apiInfo.getServerTimestamp(), notNullValue());
        assertThat(apiInfo.getRestServerUrl(), nullValue());
        assertThat(apiInfo.getWebSocketServerUrl(), is(wsBaseUri() + "/websocket"));
    }

    @Test
    public void should_return_OAUTH_config() throws Exception {
        ApiConfigVO apiConfig = performRequest("info/config/auth", "GET", emptyMap(), emptyMap(), null, OK, ApiConfigVO.class);
        assertThat(apiConfig, notNullValue());
        assertThat(apiConfig.getProviderConfigs(), notNullValue());
        assertThat(apiConfig.getProviderConfigs().size(), is(4));
        assertThat(apiConfig.getProviderConfigs(),
                hasItems(
                        new IdentityProviderConfig("google", "google_id"),
                        new IdentityProviderConfig("facebook", "facebook_id"),
                        new IdentityProviderConfig("github", "github_id"),
                        new IdentityProviderConfig("password", "")));
        assertThat(apiConfig.getSessionTimeout(), is(1200L));
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
