package com.devicehive.base;

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

import com.devicehive.application.DeviceHiveFrontendApplication;
import com.devicehive.resource.converters.CollectionProvider;
import com.devicehive.resource.converters.HiveEntityProvider;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.CollectionUtils;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {DeviceHiveFrontendApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
@TestPropertySource(locations={"classpath:application-test.properties", "classpath:application-test-configuration.properties"})
public abstract class AbstractResourceTest extends AbstractSpringKafkaTest {
    public static final String VALID_PASSWORD = "123456";
    public static final String INVALID_PASSWORD = "12345";
    public static final String ADMIN_LOGIN = "test_admin";
    public static final String ADMIN_PASS = "admin_pass";
    public static final String ADMIN_JWT = "eyJhbGciOiJIUzI1NiJ9.eyJwYXlsb2FkIjp7InUiOjEsImEiOlswXSwibiI6WyIqIl0sImQiOlsiKiJdLCJlIjoxNTU5NDExOTQwMDAwLCJ0IjoxfX0.h74Nn2pSbaN1PxrrF8KfohXeGoGpJ4au4YBpHXyvVsA";

    @LocalServerPort
    protected Integer port;

    private String httpBaseUri;
    private String wsBaseUrl;
    private WebTarget target;
    private static Client client;

    @Autowired
    protected Gson gson;

    @Before
    public void initSpringBootIntegrationTest() {
        httpBaseUri = "http://localhost:" + port + "/dh";
        wsBaseUrl = "ws://localhost:" + port + "/dh";
        client = ClientBuilder.newClient();
        client.register(HiveEntityProvider.class);
        client.register(CollectionProvider.class);
        target = client.target(httpBaseUri).path("rest");
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        client.close();
    }

    protected WebTarget target() {
        return target;
    }

    protected String baseUri() {
        return httpBaseUri;
    }

    protected String wsBaseUri() {
        return wsBaseUrl;
    }

    protected String tokenAuthHeader(String key) {
        return "Bearer " + key;
    }

    @SuppressWarnings("unchecked")
    protected final <T> T performRequest(String path, String method, Map<String, Object> params, Map<String, String> headers, Object body,
                                         Response.Status expectedStatus, Class<T> responseClass) {
        WebTarget wt = target;

        if (StringUtils.isNoneBlank(path)) {
            wt = wt.path(path);
        }

        if (!CollectionUtils.isEmpty(params)) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                wt = wt.queryParam(entry.getKey(), entry.getValue());
            }
        }

        Invocation.Builder builder = wt.request(MediaType.APPLICATION_JSON_TYPE);

        if (!CollectionUtils.isEmpty(headers)) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder = builder.header(entry.getKey(), entry.getValue());
            }
        }

        if (StringUtils.isBlank(method)) {
            method = "GET";
        }

        final Response response;
        switch (method.toUpperCase()) {
            case "GET":
                response = builder.get();
                break;
            case "POST":
                Entity<String> entity = createJsonEntity(body);
                response = builder.post(entity);
                break;
            case "PUT":
                response = builder.put(createJsonEntity(body));
                break;
            case "DELETE":
                response = builder.delete();
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown http method '%s'", method));
        }

        if (expectedStatus != null) {
            assertThat(response.getStatus(), is(expectedStatus.getStatusCode()));
        }
        if (responseClass == null || Response.class.isAssignableFrom(responseClass)) {
            return (T) response;
        }
        return response.readEntity(responseClass);
    }

    private Entity<String> createJsonEntity(Object body) {
        String val = gson.toJson(body);
        return Entity.json(val);
    }
}
