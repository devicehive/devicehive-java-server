package com.devicehive.resource;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.configuration.Constants;
import com.devicehive.model.ApiInfo;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class ApiInfoResourceTest extends AbstractResourceTest {

    @Test
    public void should_return_API_info() throws Exception {
        Response response = target.path("info")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertThat(response.getStatus(), is(OK.getStatusCode()));
        ApiInfo apiInfo = response.readEntity(ApiInfo.class);
        assertThat(apiInfo.getApiVersion(), is(Constants.API_VERSION));
        assertThat(apiInfo.getServerTimestamp(), notNullValue());
        assertThat(apiInfo.getRestServerUrl(), nullValue());
        assertThat(apiInfo.getWebSocketServerUrl(), nullValue());
    }
}
