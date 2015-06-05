package com.devicehive.base;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.json.GsonFactory;
import com.devicehive.resource.converters.CollectionProvider;
import com.devicehive.resource.converters.HiveEntityProvider;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.CollectionUtils;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DeviceHiveApplication.class)
@WebAppConfiguration
@IntegrationTest
//@TestPropertySource("classpath:application-test.properties")
public abstract class AbstractResourceTest {
    public static final String ADMIN_LOGIN = "test_admin";
    public static final String ADMIN_PASS = "admin_pass";

    @ClassRule
    public static EmbeddedKafkaClusterRule kafkaClusterRule = new EmbeddedKafkaClusterRule();

    @ClassRule
    public static EmbeddedRedisRule redisRule = new EmbeddedRedisRule();

    @Autowired
    private Environment env;

    private String baseUri;
    private WebTarget target;

    private Gson gson = GsonFactory.createGson();

    @Before
    public void initSpringBootIntegrationTest() {
        baseUri = "http://localhost:" + env.getProperty("server.port");
        Client client = ClientBuilder.newClient();
        client.register(HiveEntityProvider.class);
        client.register(CollectionProvider.class);
        target = client.target(baseUri).path("rest");
    }

    protected WebTarget target() {
        return target;
    }

    protected String baseUri() {
        return baseUri;
    }

    protected String basicAuthHeader(String login, String password) {
        String str = String.format("%s:%s", login, password);
        String base64 = Base64.getEncoder().encodeToString(str.getBytes());
        return String.format("Basic %s", base64);
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
                response = builder.post(createJsonEntity(body));
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
