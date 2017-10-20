package com.devicehive;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.configuration.Constants;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.service.NetworkService;
import com.devicehive.service.UserService;
import com.devicehive.service.security.jwt.BaseJwtClientService;
import com.devicehive.vo.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Set;
import java.util.UUID;

import static com.devicehive.auth.HiveAction.ANY;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class FrontendSmokeTest extends AbstractResourceTest {

//    @Autowired
//    private Environment env;
//
//    @Autowired
//    private NetworkService networkService;
//
//    @Autowired
//    private BaseJwtClientService jwtClientService;
//
//    @Autowired
//    private UserService userService;
//
//    @Test
//    public void should_return_API_info() throws Exception {
//        ApiInfoVO apiInfo = performRequest("info", "GET", emptyMap(), emptyMap(), null, OK, ApiInfoVO.class);
//        assertThat(apiInfo.getServerTimestamp(), notNullValue());
//        assertThat(apiInfo.getRestServerUrl(), nullValue());
//        assertThat(apiInfo.getWebSocketServerUrl(), is(wsBaseUri() + "/websocket"));
//    }
//
//    @Test
//    public void should_return_cluster_config() throws Exception {
//        ClusterConfigVO clusterConfig = performRequest("info/config/cluster", "GET", emptyMap(), emptyMap(), null, OK, ClusterConfigVO.class);
//        assertThat(clusterConfig, notNullValue());
//        assertThat(clusterConfig.getBootstrapServers(), is(env.getProperty(Constants.BOOTSTRAP_SERVERS)));
//        assertThat(clusterConfig.getZookeeperConnect(), is(env.getProperty(Constants.ZOOKEEPER_CONNECT)));
//    }
//
//    @Test
//    public void should_create_token_with_admin_permissions_for_admin_user() throws Exception {
//        // Create test user
//        UserVO user = new UserVO();
//        user.setRole(UserRole.ADMIN);
//        user.setLogin(RandomStringUtils.randomAlphabetic(10));
//        user.setStatus(UserStatus.ACTIVE);
//        user = userService.createUser(user, VALID_PASSWORD);
//
//        // Get JWT token for the user
//        JwtRequestVO jwtRequestVO = new JwtRequestVO();
//        jwtRequestVO.setLogin(user.getLogin());
//        jwtRequestVO.setPassword(VALID_PASSWORD);
//        JwtTokenVO jwtTokenVO = performRequest("/token", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), jwtRequestVO, CREATED, JwtTokenVO.class);
//
//        // Check the given user rights
//        assertNotNull(jwtTokenVO.getAccessToken());
//        JwtPayload payload = jwtClientService.getPayload(jwtTokenVO.getAccessToken());
//        assertThat(payload.getActions(), hasItem(ANY.getId()));
//        assertThat(payload.getNetworkIds(), hasItem("*"));
//        assertThat(payload.getDeviceIds(), hasItem("*"));
//    }
//
//    @Test
//    public void should_create_token_with_client_permissions_for_client_user() throws Exception {
//        // Create test user
//        UserVO user = new UserVO();
//        user.setRole(UserRole.CLIENT);
//        user.setLogin(RandomStringUtils.randomAlphabetic(10));
//        user.setStatus(UserStatus.ACTIVE);
//        user = userService.createUser(user, VALID_PASSWORD);
//
//        // Get JWT token for the user
//        JwtRequestVO jwtRequestVO = new JwtRequestVO();
//        jwtRequestVO.setLogin(user.getLogin());
//        jwtRequestVO.setPassword(VALID_PASSWORD);
//        JwtTokenVO jwtTokenVO = performRequest("/token", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), jwtRequestVO, CREATED, JwtTokenVO.class);
//
//        // Check the given user rights
//        assertNotNull(jwtTokenVO.getAccessToken());
//        JwtPayload payload = jwtClientService.getPayload(jwtTokenVO.getAccessToken());
//        Set<String> networkIds = payload.getNetworkIds();
//        if (networkIds != null && !networkIds.isEmpty()) {
//            assertThat(payload.getDeviceIds(), hasItem("*"));
//        }
//    }
//
//    @Test
//    public void should_save_device_as_admin() {
//        NetworkVO network = DeviceFixture.createNetwork();
//        network.setName("" + randomUUID());
//        NetworkVO created = networkService.create(network);
//
//        String deviceId = UUID.randomUUID().toString();
//        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(deviceId);
//        deviceUpdate.setNetworkId(created.getId());
//
//        // register device
//        Response response = performRequest("/device/" + deviceId, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), deviceUpdate, NO_CONTENT, null);
//        assertNotNull(response);
//
//        // get device
//        DeviceVO device = performRequest("/device/" + deviceId, "GET", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), deviceUpdate, OK, DeviceVO.class);
//        assertNotNull(device);
//        assertThat(device.getDeviceId(), is(deviceId));
//        assertThat(device.getName(), is(device.getName()));
//        assertThat(device.getData(), notNullValue());
//        assertThat(device.getNetworkId(), notNullValue());
//    }
//
//    @Test
//    public void should_return_error_code_if_user_is_disabled_for_token_auth() throws Exception {
//        String login = RandomStringUtils.randomAlphabetic(10);
//        String password = RandomStringUtils.randomAlphabetic(10);
//
//        UserUpdate testUser = new UserUpdate();
//        testUser.setLogin(login);
//        testUser.setRole(UserRole.CLIENT.getValue());
//        testUser.setPassword(password);
//        testUser.setStatus(UserStatus.ACTIVE.getValue());
//
//        UserVO user = performRequest("/user", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), testUser, CREATED, UserVO.class);
//        assertThat(user.getId(), notNullValue());
//
//        final long userid = user.getId();
//        user = performRequest("/user/" + user.getId(), "GET", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), null, OK, UserVO.class);
//        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));
//        assertThat(user.getId(), equalTo(userid));
//
//        testUser = new UserUpdate();
//        testUser.setStatus(UserStatus.DISABLED.getValue());
//        testUser.setLogin(login);
//        testUser.setPassword(password);
//        performRequest("/user/" + user.getId(), "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), testUser, NO_CONTENT, Response.class);
//
//        user = performRequest("/user/" + user.getId(), "GET", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), null, OK, UserVO.class);
//        assertThat(user.getStatus(), equalTo(UserStatus.DISABLED));
//    }
}
