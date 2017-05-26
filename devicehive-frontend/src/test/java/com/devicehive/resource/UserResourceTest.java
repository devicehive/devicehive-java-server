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
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.vo.UserVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class UserResourceTest extends AbstractResourceTest {

    @Test
    public void should_return_error_code_if_user_is_disabled_for_token_auth() throws Exception {
        String login = RandomStringUtils.randomAlphabetic(10);
        String password = RandomStringUtils.randomAlphabetic(10);

        UserUpdate testUser = new UserUpdate();
        testUser.setLogin(login);
        testUser.setRole(UserRole.CLIENT.getValue());
        testUser.setPassword(password);
        testUser.setStatus(UserStatus.ACTIVE.getValue());

        UserVO user = performRequest("/user", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), testUser, CREATED, UserVO.class);
        assertThat(user.getId(), notNullValue());

        final long userid = user.getId();
        user = performRequest("/user/" + user.getId(), "GET", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), null, OK, UserVO.class);
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));
        assertThat(user.getId(), equalTo(userid));

        testUser = new UserUpdate();
        testUser.setStatus(UserStatus.DISABLED.getValue());
        testUser.setLogin(login);
        testUser.setPassword(password);
        performRequest("/user/" + user.getId(), "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), testUser, NO_CONTENT, Response.class);

        user = performRequest("/user/" + user.getId(), "GET", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)), null, OK, UserVO.class);
        assertThat(user.getStatus(), equalTo(UserStatus.DISABLED));
    }

}
