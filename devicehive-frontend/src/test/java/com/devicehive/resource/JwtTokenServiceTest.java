package com.devicehive.resource;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.service.UserService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.vo.JwtRequestVO;
import com.devicehive.vo.JwtTokenVO;
import com.devicehive.vo.UserVO;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import javax.ws.rs.core.HttpHeaders;
import java.util.*;

import static com.devicehive.auth.HiveAction.ANY;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class JwtTokenServiceTest extends AbstractResourceTest {

    @Autowired
    private JwtClientService jwtClientService;

    @Autowired
    private UserService userService;

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_create_token_with_admin_permissions_for_admin_user() throws Exception {
        // Create test user
        UserVO user = new UserVO();
        user.setRole(UserRole.ADMIN);
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, VALID_PASSWORD);

        // Get JWT token for the user
        JwtRequestVO jwtRequestVO = new JwtRequestVO();
        jwtRequestVO.setLogin(user.getLogin());
        jwtRequestVO.setPassword(VALID_PASSWORD);
        JwtTokenVO jwtTokenVO = performRequest("/token", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), jwtRequestVO, CREATED, JwtTokenVO.class);

        // Check the given user rights
        assertNotNull(jwtTokenVO.getAccessToken());
        JwtPayload payload = jwtClientService.getPayload(jwtTokenVO.getAccessToken());
        assertThat(payload.getActions(), hasItem(ANY.getId()));
        assertThat(payload.getNetworkIds(), hasItem("*"));
        assertThat(payload.getDeviceIds(), hasItem("*"));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_create_token_with_client_permissions_for_client_user() throws Exception {
        // Create test user
        UserVO user = new UserVO();
        user.setRole(UserRole.CLIENT);
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, VALID_PASSWORD);

        // Get JWT token for the user
        JwtRequestVO jwtRequestVO = new JwtRequestVO();
        jwtRequestVO.setLogin(user.getLogin());
        jwtRequestVO.setPassword(VALID_PASSWORD);
        JwtTokenVO jwtTokenVO = performRequest("/token", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), jwtRequestVO, CREATED, JwtTokenVO.class);

        // Check the given user rights
        assertNotNull(jwtTokenVO.getAccessToken());
        JwtPayload payload = jwtClientService.getPayload(jwtTokenVO.getAccessToken());
        Set<String> networkIds = payload.getNetworkIds();
        if (networkIds != null && !networkIds.isEmpty()) {
        	assertThat(payload.getDeviceIds(), hasItem("*"));
        }
    }

}
