package com.devicehive.resource;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.configuration.Constants;
import com.devicehive.model.ApiConfig;
import com.devicehive.model.ApiInfo;
import com.devicehive.model.ClusterConfig;
import com.devicehive.model.IdentityProviderConfig;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ApiInfoResourceTest extends AbstractResourceTest {

    @Autowired
    private Environment env;

    @Test
    public void should_return_API_info() throws Exception {
        Response response = target().path("info")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertThat(response.getStatus(), is(OK.getStatusCode()));
        ApiInfo apiInfo = response.readEntity(ApiInfo.class);
        assertThat(apiInfo.getApiVersion(), is(Constants.API_VERSION));
        assertThat(apiInfo.getServerTimestamp(), notNullValue());
        assertThat(apiInfo.getRestServerUrl(), nullValue());
        assertThat(apiInfo.getWebSocketServerUrl(), nullValue());

        //configure rest.url and websocket.url
        response = target().path("configuration")
                .path(Constants.REST_SERVER_URL)
                .queryParam("value", baseUri() + "/rest")
                .request()
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS))
                .get();
        assertThat(response.getStatus(), is(OK.getStatusCode()));
        response = target().path("configuration")
                .path(Constants.WEBSOCKET_SERVER_URL)
                .path("set")
                .queryParam("value", baseUri() + "/websocket")
                .request()
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader(ADMIN_LOGIN, ADMIN_PASS))
                .get();
        assertThat(response.getStatus(), is(OK.getStatusCode()));

        response = target().path("info")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertThat(response.getStatus(), is(OK.getStatusCode()));
        apiInfo = response.readEntity(ApiInfo.class);

        assertThat(apiInfo.getApiVersion(), is(Constants.API_VERSION));
        assertThat(apiInfo.getServerTimestamp(), notNullValue());
        assertThat(apiInfo.getRestServerUrl(), nullValue());
        assertThat(apiInfo.getWebSocketServerUrl(), is(baseUri() + "/websocket"));
    }

    @Test
    public void should_return_OAUTH_config() throws Exception {
        Response response = target().path("info/config/auth")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertThat(response.getStatus(), is(OK.getStatusCode()));
        ApiConfig apiConfig = response.readEntity(ApiConfig.class);
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
        Response response = target().path("info/config/cluster")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertThat(response.getStatus(), is(OK.getStatusCode()));
        ClusterConfig clusterConfig = response.readEntity(ClusterConfig.class);
        assertThat(clusterConfig, notNullValue());
        assertThat(clusterConfig.getMetadataBrokerList(), is(env.getProperty(Constants.METADATA_BROKER_LIST)));
        assertThat(clusterConfig.getZookeeperConnect(), is(env.getProperty(Constants.ZOOKEEPER_CONNECT)));
        assertThat(clusterConfig.getCassandraContactpoints(), is(env.getProperty(Constants.CASSANDRA_CONTACTPOINTS)));
        assertThat(clusterConfig.getThreadsCount(), is(1));
    }
}
