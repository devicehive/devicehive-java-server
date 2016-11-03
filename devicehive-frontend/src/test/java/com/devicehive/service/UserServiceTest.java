package com.devicehive.service;

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
import com.devicehive.base.RequestDispatcherProxy;
import com.devicehive.model.rpc.ListUserRequest;
import com.devicehive.model.rpc.ListUserResponse;
import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.UserDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;
import com.devicehive.vo.UserWithNetworkVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;

public class UserServiceTest extends AbstractResourceTest {

    @Autowired
    private UserService userService;
    @Autowired
    private NetworkService networkService;
    @Autowired
    private UserDao userDao;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private RequestDispatcherProxy requestDispatcherProxy;

    @Mock
    private RequestHandler requestHandler;

    private ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        requestDispatcherProxy.setRequestHandler(requestHandler);
    }

    @After
    public void tearDown() {
        Mockito.reset(requestHandler);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void should_throw_NoSuchElementException_if_user_is_null_when_assign_network() throws Exception {
        NetworkVO network = new NetworkVO();
        String networkName = RandomStringUtils.randomAlphabetic(10);
        network.setName(networkName);
        NetworkVO created = networkService.create(network);
        assertThat(created, notNullValue());
        assertThat(created.getId(), notNullValue());

        expectedException.expect(NoSuchElementException.class);
        expectedException.expectMessage(Messages.USER_NOT_FOUND);

        userService.assignNetwork(-1L, created.getId());
    }

    @Test
    public void should_throw_NoSuchElementException_if_network_is_null_when_assign_network() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());

        expectedException.expect(NoSuchElementException.class);
        expectedException.expectMessage(String.format(Messages.NETWORK_NOT_FOUND, -1));

        userService.assignNetwork(user.getId(), -1L);
    }

    @Test
    public void should_assign_network_to_user() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());

        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        NetworkVO first = networkService.create(network);
        assertThat(first, notNullValue());
        assertThat(first.getId(), notNullValue());

        userService.assignNetwork(user.getId(), first.getId());

        UserWithNetworkVO user1 = userService.findUserWithNetworks(user.getId());
        assertThat(user1.getNetworks(), notNullValue());
        assertThat(user1.getNetworks(), hasSize(1));
        assertThat(user1.getNetworks(), hasItem(new CustomTypeSafeMatcher<NetworkVO>("expect network") {
            @Override
            protected boolean matchesSafely(NetworkVO item) {
                return first.getId().equals(item.getId()) && first.getName().equals(item.getName());
            }
        }));

        network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        NetworkVO second = networkService.create(network);
        assertThat(second, notNullValue());
        assertThat(second.getId(), notNullValue());

        userService.assignNetwork(user.getId(), second.getId());

        user1 = userService.findUserWithNetworks(user.getId());
        assertThat(user1.getNetworks(), notNullValue());
        assertThat(user1.getNetworks(), hasSize(2));
        assertThat(user1.getNetworks(),
                hasItems(
                        new CustomTypeSafeMatcher<NetworkVO>("expect network") {
                            @Override
                            protected boolean matchesSafely(NetworkVO item) {
                                return first.getId().equals(item.getId()) && first.getName().equals(item.getName());
                            }
                        },
                        new CustomTypeSafeMatcher<NetworkVO>("expect network") {
                            @Override
                            protected boolean matchesSafely(NetworkVO item) {
                                return second.getId().equals(item.getId()) && second.getName().equals(item.getName());
                            }
                        }
                ));
    }

    @Test
    public void should_unassign_network_from_user() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());

        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        NetworkVO first = networkService.create(network);
        assertThat(first, notNullValue());
        assertThat(first.getId(), notNullValue());
        userService.assignNetwork(user.getId(), first.getId());
        UserWithNetworkVO user1 = userService.findUserWithNetworks(user.getId());
        assertThat(user1.getNetworks(), notNullValue());
        assertThat(user1.getNetworks(), hasSize(1));

        network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        NetworkVO second = networkService.create(network);
        assertThat(second, notNullValue());
        assertThat(second.getId(), notNullValue());
        userService.assignNetwork(user.getId(), second.getId());
        user1 = userService.findUserWithNetworks(user.getId());
        assertThat(user1.getNetworks(), notNullValue());
        assertThat(user1.getNetworks(), hasSize(2));

        userService.unassignNetwork(user.getId(), first.getId());

        user1 = userService.findUserWithNetworks(user.getId());
        assertThat(user1.getNetworks(), hasSize(1));
        assertThat(user1.getNetworks(), hasItem(new CustomTypeSafeMatcher<NetworkVO>("expect network") {
            @Override
            protected boolean matchesSafely(NetworkVO item) {
                return second.getId().equals(item.getId()) && second.getName().equals(item.getName());
            }

        }));

        userService.unassignNetwork(user.getId(), second.getId());
        user1 = userService.findUserWithNetworks(user.getId());
        assertThat(user1.getNetworks(), is(empty()));
    }

    @Test
    public void should_throw_NoSuchElementException_if_user_is_null_when_unassign_network() throws Exception {
        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        NetworkVO created = networkService.create(network);
        assertThat(created, notNullValue());
        assertThat(created.getId(), notNullValue());

        expectedException.expect(NoSuchElementException.class);
        expectedException.expectMessage(Messages.USER_NOT_FOUND);

        userService.unassignNetwork(-1, created.getId());
    }

    @Test
    public void should_do_nothing_if_network_is_null_when_unassign_user_from_network() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());

        userService.unassignNetwork(user.getId(), -1);
    }

    @Test
    public void should_return_null_if_user_does_not_exist_when_authenticate() throws Exception {
        UserVO user = userService.authenticate(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10));
        assertThat(user, nullValue());
    }

    @Test
    public void should_authenticate_user_successfully() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));

        UserVO authenticated = userService.authenticate(user.getLogin(), "123");
        assertThat(authenticated, notNullValue());
        assertThat(authenticated.getLoginAttempts(), equalTo(0));
        assertThat(authenticated.getLastLogin(), notNullValue());
    }

    @Test
    public void should_increase_login_attempts_if_user_failed_to_login() throws Exception {
        UserVO newUser = new UserVO();
        newUser.setLogin(RandomStringUtils.randomAlphabetic(10));
        newUser.setStatus(UserStatus.ACTIVE);
        UserVO user = userService.createUser(newUser, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));

        IntStream.range(1, 5).forEach(attempt -> {
            try {
                userService.authenticate(user.getLogin(), "wrong_password");
                fail("should throw login exception");
            } catch (ActionNotAllowedException e) {
                assertThat(e.getMessage(), equalTo(String.format(Messages.INCORRECT_CREDENTIALS, user.getLogin())));
            }
            UserVO updatedUser = userDao.find(user.getId());
            assertThat(updatedUser, notNullValue());
            assertThat(updatedUser.getLoginAttempts(), equalTo(attempt));
            assertThat(updatedUser.getStatus(), equalTo(UserStatus.ACTIVE));
            assertThat(updatedUser.getLastLogin(), nullValue());
        });
    }

    @Test
    public void should_lock_user_if_max_login_attempts_reached() throws Exception {
        UserVO newUser = new UserVO();
        newUser.setLogin(RandomStringUtils.randomAlphabetic(10));
        newUser.setStatus(UserStatus.ACTIVE);
        UserVO user = userService.createUser(newUser, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));

        configurationService.save(Constants.MAX_LOGIN_ATTEMPTS, 5);

        IntStream.range(1, 5).forEach(attempt -> {
            try {
                userService.authenticate(user.getLogin(), "wrong_password");
                fail("should throw login exception");
            } catch (ActionNotAllowedException e) {
                assertThat(e.getMessage(), equalTo(String.format(Messages.INCORRECT_CREDENTIALS, user.getLogin())));
            }
            UserVO updatedUser = userDao.find(user.getId());
            assertThat(updatedUser, notNullValue());
            assertThat(updatedUser.getLoginAttempts(), equalTo(attempt));
            assertThat(updatedUser.getStatus(), equalTo(UserStatus.ACTIVE));
            assertThat(updatedUser.getLastLogin(), nullValue());
        });

        try {
            userService.authenticate(user.getLogin(), "wrong_password");
            fail("should throw login exception");
        } catch (ActionNotAllowedException e) {
            assertThat(e.getMessage(), equalTo(String.format(Messages.INCORRECT_CREDENTIALS, user.getLogin())));
        }
        UserVO updatedUser = userDao.find(user.getId());
        assertThat(updatedUser, notNullValue());
        assertThat(updatedUser.getLoginAttempts(), equalTo(0));
        assertThat(updatedUser.getStatus(), equalTo(UserStatus.LOCKED_OUT));
        assertThat(updatedUser.getLastLogin(), nullValue());
    }

    @Test
    public void should_authenticate_user_after_not_successful_authentication() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));

        UserVO authenticated = userService.authenticate(user.getLogin(), "123");
        assertThat(authenticated, notNullValue());
        assertThat(authenticated.getLoginAttempts(), equalTo(0));
        assertThat(authenticated.getLastLogin(), notNullValue());

        try {
            userService.authenticate(user.getLogin(), "1234");
            fail("should throw login exception");
        } catch (ActionNotAllowedException e) {
            assertThat(e.getMessage(), equalTo(String.format(Messages.INCORRECT_CREDENTIALS, user.getLogin())));
        }
        UserVO updatedUser = userDao.find(user.getId());
        assertThat(updatedUser, notNullValue());
        assertThat(updatedUser.getLoginAttempts(), equalTo(1));
        assertThat(updatedUser.getStatus(), equalTo(UserStatus.ACTIVE));
        assertThat(updatedUser.getLastLogin(), notNullValue());

        authenticated = userService.authenticate(user.getLogin(), "123");
        assertThat(authenticated, notNullValue());
        assertThat(authenticated.getLoginAttempts(), equalTo(0));
        assertThat(authenticated.getLastLogin(), notNullValue());
    }

    @Test
    public void should_not_modify_last_login_within_login_timeout() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));

        configurationService.save(Constants.LAST_LOGIN_TIMEOUT, 20000); //login timeout 20 sec

        UserVO authenticated1 = userService.authenticate(user.getLogin(), "123");
        UserVO authenticated2 = userService.authenticate(user.getLogin(), "123");

        assertThat(authenticated1.getId(), equalTo(authenticated2.getId()));
        assertThat(authenticated1.getLogin(), equalTo(authenticated2.getLogin()));
        assertThat(authenticated1.getLastLogin(), equalTo(authenticated2.getLastLogin()));
        assertThat(authenticated1.getLoginAttempts(), equalTo(authenticated2.getLoginAttempts()));
    }

    @Test
    public void should_modify_last_login_if_login_timeout_is_reached() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));

        configurationService.save(Constants.LAST_LOGIN_TIMEOUT, 0);
        UserVO authenticated1 = userService.authenticate(user.getLogin(), "123");
        UserVO authenticated2 = userService.authenticate(user.getLogin(), "123");

        assertThat(authenticated1.getId(), equalTo(authenticated2.getId()));
        assertThat(authenticated1.getLogin(), equalTo(authenticated2.getLogin()));
        assertThat(authenticated1.getLastLogin(), not(equalTo(authenticated2.getLastLogin())));
        assertTrue(authenticated1.getLastLogin().before(authenticated2.getLastLogin()));
        assertThat(authenticated1.getLoginAttempts(), equalTo(authenticated2.getLoginAttempts()));
    }

    @Test
    public void should_throw_AccessDeniedException_if_user_does_not_exists_when_findUser_called() throws Exception {
        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(Messages.USER_NOT_FOUND);
        try {
            userService.findUser(String.valueOf(System.currentTimeMillis()), String.valueOf(System.currentTimeMillis()));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void should_throw_AccessDeniedException_if_user_is_disabled_when_findUser_called() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.DISABLED);
        user = userService.createUser(user, "123");

        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(Messages.USER_NOT_ACTIVE);

        userService.findUser(user.getLogin(), "123");
    }

    @Test
    public void should_throw_AccessDeniedException_if_password_is_wrong_when_funcUser_called() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));

        try {
            userService.findUser(user.getLogin(), "wrong_password");
            fail("should throw AccessDeniedException exception");
        } catch (AccessDeniedException e) {
            assertThat(e.getMessage(), equalTo(String.format(Messages.INCORRECT_CREDENTIALS, user.getLogin())));
        }

        UserVO updatedUser = userDao.find(user.getId());
        assertThat(updatedUser, notNullValue());
        assertThat(updatedUser.getLoginAttempts(), equalTo(1));
        assertThat(updatedUser.getStatus(), equalTo(UserStatus.ACTIVE));
        assertThat(updatedUser.getLastLogin(), nullValue());

        user = userService.findUser(user.getLogin(), "123");
        assertThat(user, notNullValue());
        assertThat(user.getLoginAttempts(), equalTo(0));
        assertThat(user.getLastLogin(), notNullValue());
    }

    @Test
    public void should_update_login_statistic_on_successful_login_when_findUser_called() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));

        user = userService.findUser(user.getLogin(), "123");
        assertThat(user, notNullValue());
        assertThat(user.getLoginAttempts(), equalTo(0));
        assertThat(user.getLastLogin(), notNullValue());
    }

    @Test
    public void should_lock_user_if_max_login_attempts_reached_when_findUser_called() throws Exception {
        UserVO newUser = new UserVO();
        newUser.setLogin(RandomStringUtils.randomAlphabetic(10));
        newUser.setStatus(UserStatus.ACTIVE);
        UserVO user = userService.createUser(newUser, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));

        configurationService.save(Constants.MAX_LOGIN_ATTEMPTS, 5);

        IntStream.range(1, 5).forEach(attempt -> {
            try {
                userService.findUser(user.getLogin(), "wrong_password");
                fail("should throw login exception");
            } catch (AccessDeniedException e) {
                assertThat(e.getMessage(), equalTo(String.format(Messages.INCORRECT_CREDENTIALS, user.getLogin())));
            }
            UserVO updatedUser = userDao.find(user.getId());
            assertThat(updatedUser, notNullValue());
            assertThat(updatedUser.getLoginAttempts(), equalTo(attempt));
            assertThat(updatedUser.getStatus(), equalTo(UserStatus.ACTIVE));
            assertThat(updatedUser.getLastLogin(), nullValue());
        });

        try {
            userService.findUser(user.getLogin(), "wrong_password");
            fail("should throw login exception");
        } catch (AccessDeniedException e) {
            assertThat(e.getMessage(), equalTo(String.format(Messages.INCORRECT_CREDENTIALS, user.getLogin())));
        }
        UserVO updatedUser = userDao.find(user.getId());
        assertThat(updatedUser, notNullValue());
        assertThat(updatedUser.getLoginAttempts(), equalTo(0));
        assertThat(updatedUser.getStatus(), equalTo(UserStatus.LOCKED_OUT));
        assertThat(updatedUser.getLastLogin(), nullValue());
    }

    @Test
    public void should_modify_last_login_if_login_timeout_is_reached_when_findUser_called() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));

        configurationService.save(Constants.LAST_LOGIN_TIMEOUT, 0);
        UserVO authenticated1 = userService.findUser(user.getLogin(), "123");
        UserVO authenticated2 = userService.findUser(user.getLogin(), "123");

        assertThat(authenticated1.getId(), equalTo(authenticated2.getId()));
        assertThat(authenticated1.getLogin(), equalTo(authenticated2.getLogin()));
        assertThat(authenticated1.getLastLogin(), not(equalTo(authenticated2.getLastLogin())));
        assertTrue(authenticated1.getLastLogin().before(authenticated2.getLastLogin()));
        assertThat(authenticated1.getLoginAttempts(), equalTo(authenticated2.getLoginAttempts()));
    }

    @Test
    public void should_return_user_by_id() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());

        user = userService.findById(user.getId());
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));
    }

    @Test
    public void should_return_user_with_networks() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());

        for (int i = 0; i < 10; i++) {
            NetworkVO network = new NetworkVO();
            network.setName(RandomStringUtils.randomAlphabetic(10));
            network = networkService.create(network);
            userService.assignNetwork(user.getId(), network.getId());
        }

        UserWithNetworkVO userWithNetworks = userService.findUserWithNetworks(user.getId());
        assertThat(userWithNetworks, notNullValue());
        assertThat(userWithNetworks.getNetworks(), notNullValue());
        assertThat(userWithNetworks.getNetworks(), not(empty()));
        assertThat(userWithNetworks.getNetworks(), hasSize(10));
    }

    @Test
    public void should_return_user_without_networks() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());

        user = userService.findUserWithNetworks(user.getId());
        assertThat(user, notNullValue());
    }

    @Test
    public void should_create_user() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);

        String googleLogin = RandomStringUtils.randomAlphabetic(10);
        user.setGoogleLogin(googleLogin);
        String facebookLogin = RandomStringUtils.randomAlphabetic(10);
        user.setFacebookLogin(facebookLogin);
        String githubLogin = RandomStringUtils.randomAlphabetic(10);
        user.setGithubLogin(githubLogin);

        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));
        assertThat(user.getGoogleLogin(), equalTo(googleLogin));
        assertThat(user.getFacebookLogin(), equalTo(facebookLogin));
        assertThat(user.getGithubLogin(), equalTo(githubLogin));
        assertThat(user.getLoginAttempts(), equalTo(0));
    }

    @Test
    public void should_throw_IllegalParametersException_if_id_provided_for_create() throws Exception {
        UserVO user = new UserVO();
        user.setId(-1L);
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);

        expectedException.expect(IllegalParametersException.class);
        expectedException.expectMessage(Messages.ID_NOT_ALLOWED);

        userService.createUser(user, "123");
    }

    @Test
    public void should_throw_ActionNotAllowedException_if_login_exists() throws Exception {
        String login = RandomStringUtils.randomAlphabetic(10);
        UserVO user = new UserVO();
        user.setLogin(login);
        user.setStatus(UserStatus.ACTIVE);

        userService.createUser(user, RandomStringUtils.randomAlphabetic(10));

        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.DUPLICATE_LOGIN);

        user = new UserVO();
        user.setLogin(login);
        user.setStatus(UserStatus.ACTIVE);

        userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
    }

    @Test
    public void should_create_user_without_password() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);

        user = userService.createUser(user, null);
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getPasswordHash(), nullValue());
        assertThat(user.getPasswordSalt(), nullValue());
    }

    @Test
    public void should_throw_ActionNotAllowedException_if_any_identity_login_already_exists() throws Exception {
        String google = RandomStringUtils.randomAlphabetic(10);
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setGoogleLogin(google);
        userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
        try {
            user = new UserVO();
            user.setLogin(RandomStringUtils.randomAlphabetic(10));
            user.setStatus(UserStatus.ACTIVE);
            user.setGoogleLogin(google);
            userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
            fail("should throw ActionNotAllowedException");
        } catch (ActionNotAllowedException e) {
            assertThat(e.getMessage(), equalTo(Messages.DUPLICATE_IDENTITY_LOGIN));
        }

        String facebook = RandomStringUtils.randomAlphabetic(10);
        user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setFacebookLogin(facebook);
        userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
        try {
            user = new UserVO();
            user.setLogin(RandomStringUtils.randomAlphabetic(10));
            user.setStatus(UserStatus.ACTIVE);
            user.setFacebookLogin(facebook);
            userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
            fail("should throw ActionNotAllowedException");
        } catch (ActionNotAllowedException e) {
            assertThat(e.getMessage(), equalTo(Messages.DUPLICATE_IDENTITY_LOGIN));
        }

        String github = RandomStringUtils.randomAlphabetic(10);
        user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setGithubLogin(github);
        userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
        try {
            user = new UserVO();
            user.setLogin(RandomStringUtils.randomAlphabetic(10));
            user.setStatus(UserStatus.ACTIVE);
            user.setGithubLogin(github);
            userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
            fail("should throw ActionNotAllowedException");
        } catch (ActionNotAllowedException e) {
            assertThat(e.getMessage(), equalTo(Messages.DUPLICATE_IDENTITY_LOGIN));
        }
    }

    @Test
    public void should_delete_user() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());

        assertTrue(userService.deleteUser(user.getId()));

        user = userService.findById(user.getId());
        assertThat(user, nullValue());
    }

    @Test
    public void should_return_true_if_user_has_access_to_device() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);

        userService.assignNetwork(user.getId(), network.getId());

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(randomUUID().toString()));

        DeviceUpdate device = new DeviceUpdate();
        device.setName(Optional.ofNullable(randomUUID().toString()));
        device.setGuid(Optional.ofNullable(randomUUID().toString()));
        device.setDeviceClass(Optional.ofNullable(dc));
        device.setNetwork(Optional.ofNullable(network));
        deviceService.deviceSave(device, Collections.emptySet());

        assertTrue(userService.hasAccessToDevice(user, device.getGuid().orElse(null)));
    }

    @Test
    public void should_return_false_if_user_does_not_have_access_to_device() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(randomUUID().toString()));

        DeviceUpdate device = new DeviceUpdate();
        device.setName(Optional.ofNullable(randomUUID().toString()));
        device.setGuid(Optional.ofNullable(randomUUID().toString()));
        device.setDeviceClass(Optional.ofNullable(dc));
        device.setNetwork(Optional.ofNullable(network));
        deviceService.deviceSave(device, Collections.emptySet());

        assertFalse(userService.hasAccessToDevice(user, device.getGuid().orElse(null)));
    }

    @Test
    public void should_return_true_if_user_is_admin_when_request_access_to_device() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);

        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(randomUUID().toString()));

        DeviceUpdate device = new DeviceUpdate();
        device.setName(Optional.ofNullable(randomUUID().toString()));
        device.setGuid(Optional.ofNullable(randomUUID().toString()));
        device.setDeviceClass(Optional.ofNullable(dc));
        device.setNetwork(Optional.ofNullable(network));
        deviceService.deviceSave(device, Collections.emptySet());

        assertTrue(userService.hasAccessToDevice(user, device.getGuid().orElse(null)));
    }

    @Test
    public void should_return_true_if_user_is_admin_when_request_access_to_network() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.ADMIN);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);

        assertTrue(userService.hasAccessToNetwork(user, network));
    }

    @Test
    public void should_return_true_if_user_has_access_to_network() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);

        userService.assignNetwork(user.getId(), network.getId());

        assertTrue(userService.hasAccessToNetwork(user, network));
    }

    @Test
    public void should_return_false_if_user_does_not_have_access_to_network() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");

        NetworkVO network = new NetworkVO();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        network = networkService.create(network);

        assertFalse(userService.hasAccessToNetwork(user, network));
    }

    @Test
    public void should_return_user_by_google_login() throws Exception {
        String google = RandomStringUtils.randomAlphabetic(10);

        UserVO user = userService.findGoogleUser(google);
        assertThat(user, nullValue());

        user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CLIENT);
        user.setGoogleLogin(google);
        userService.createUser(user, "123");

        user = userService.findGoogleUser(google);
        assertThat(user, notNullValue());
        assertThat(user.getGoogleLogin(), equalTo(google));
    }

    @Test
    public void should_return_user_by_facebook_login() throws Exception {
        String facebook = RandomStringUtils.randomAlphabetic(10);

        UserVO user = userService.findFacebookUser(facebook);
        assertThat(user, nullValue());

        user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CLIENT);
        user.setFacebookLogin(facebook);
        userService.createUser(user, "123");

        user = userService.findFacebookUser(facebook);
        assertThat(user, notNullValue());
        assertThat(user.getFacebookLogin(), equalTo(facebook));
    }

    @Test
    public void should_return_user_by_github_login() throws Exception {
        String gitgub = RandomStringUtils.randomAlphabetic(10);

        UserVO user = userService.findGithubUser(gitgub);
        assertThat(user, nullValue());

        user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CLIENT);
        user.setGithubLogin(gitgub);
        userService.createUser(user, "123");

        user = userService.findGithubUser(gitgub);
        assertThat(user, notNullValue());
        assertThat(user.getGithubLogin(), equalTo(gitgub));
    }

    @Test
    public void should_refresh_user_login_data() throws Exception {
        configurationService.save(Constants.MAX_LOGIN_ATTEMPTS, 10);
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CLIENT);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());

        user = userService.authenticate(user.getLogin(), "123");
        assertThat(user, notNullValue());
        assertThat(user.getLastLogin(), notNullValue());
        long lastLogin = user.getLastLogin().getTime();

        for (int i = 0; i < 5; i++) {
            try {
                userService.authenticate(user.getLogin(), "wrong_password");
                fail("should throw ActionNotAllowedException");
            } catch (ActionNotAllowedException e) { }
        }

        user = userService.findById(user.getId());
        assertThat(user.getLoginAttempts(), equalTo(5));
        assertThat(user.getLastLogin().getTime(), equalTo(lastLogin));

        configurationService.save(Constants.LAST_LOGIN_TIMEOUT, 0);
        user = userService.refreshUserLoginData(user);
        assertThat(user.getLoginAttempts(), equalTo(0));
        assertThat(user.getLastLogin().getTime(), not(equalTo(lastLogin)));
        assertTrue(new Timestamp(lastLogin).before(user.getLastLogin()));
    }

    @Test
    public void should_throw_NoSuchElementException_if_user_does_not_exist_when_update() throws Exception {
        expectedException.expect(NoSuchElementException.class);
        expectedException.expectMessage(Messages.USER_NOT_FOUND);

        userService.updateUser(-1L, new UserUpdate(), UserRole.ADMIN);
    }

    @Test
    public void should_throw_ActionNotAllowedException_trying_to_set_existing_login_when_update() throws Exception {
        String existingLogin = RandomStringUtils.randomAlphabetic(10);

        UserVO first = new UserVO();
        first.setLogin(existingLogin);
        first.setStatus(UserStatus.ACTIVE);
        first.setRole(UserRole.CLIENT);
        userService.createUser(first, "123");

        UserVO second = new UserVO();
        second.setLogin(RandomStringUtils.randomAlphabetic(10));
        second.setStatus(UserStatus.ACTIVE);
        second.setRole(UserRole.CLIENT);
        second = userService.createUser(second, "123");

        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.DUPLICATE_LOGIN);

        UserUpdate update = new UserUpdate();
        update.setLogin(Optional.ofNullable(existingLogin));
        userService.updateUser(second.getId(), update, UserRole.ADMIN);
    }

    @Test
    public void should_update_user_identity_logins() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.CLIENT);
        user.setFacebookLogin(RandomStringUtils.randomAlphabetic(10));
        user.setGoogleLogin(RandomStringUtils.randomAlphabetic(10));
        user.setGithubLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");

        UserUpdate update = new UserUpdate();
        update.setLogin(Optional.ofNullable(RandomStringUtils.random(10)));
        update.setFacebookLogin(Optional.ofNullable(RandomStringUtils.random(10)));
        update.setGoogleLogin(Optional.ofNullable(RandomStringUtils.random(10)));
        update.setGithubLogin(Optional.ofNullable(RandomStringUtils.random(10)));

        UserVO updatedUser = userService.updateUser(user.getId(), update, UserRole.ADMIN);
        assertThat(updatedUser, notNullValue());
        assertThat(updatedUser.getId(), equalTo(user.getId()));
        assertThat(updatedUser.getLogin(), allOf(not(equalTo(user.getLogin())), equalTo(update.getLogin().orElse(null))));
        assertThat(updatedUser.getFacebookLogin(), allOf(not(equalTo(user.getFacebookLogin())), equalTo(update.getFacebookLogin().orElse(null))));
        assertThat(updatedUser.getGoogleLogin(), allOf(not(equalTo(user.getGoogleLogin())), equalTo(update.getGoogleLogin().orElse(null))));
        assertThat(updatedUser.getGithubLogin(), allOf(not(equalTo(user.getGithubLogin())), equalTo(update.getGithubLogin().orElse(null))));
    }

    @Test
    public void should_throw_ActionNotAllowedException_if_user_with_any_login_exists_when_update() throws Exception {
        String google = RandomStringUtils.randomAlphabetic(10);
        UserVO firstGoogleUser = new UserVO();
        firstGoogleUser.setLogin(RandomStringUtils.randomAlphabetic(10));
        firstGoogleUser.setGoogleLogin(google);
        firstGoogleUser.setStatus(UserStatus.ACTIVE);
        firstGoogleUser = userService.createUser(firstGoogleUser, RandomStringUtils.randomAlphabetic(10));
        assertThat(firstGoogleUser.getGoogleLogin(), equalTo(google));

        UserVO secondGoogleUser = new UserVO();
        secondGoogleUser.setLogin(RandomStringUtils.randomAlphabetic(10));
        secondGoogleUser.setGoogleLogin(RandomStringUtils.randomAlphabetic(10));
        secondGoogleUser.setStatus(UserStatus.ACTIVE);
        secondGoogleUser = userService.createUser(secondGoogleUser, RandomStringUtils.randomAlphabetic(10));
        assertThat(firstGoogleUser.getId(), not(equalTo(secondGoogleUser.getId())));
        try {
            UserUpdate update = new UserUpdate();
            update.setLogin(Optional.ofNullable(secondGoogleUser.getLogin()));
            update.setGoogleLogin(Optional.ofNullable(google));
            update.setFacebookLogin(Optional.ofNullable(null));
            update.setGithubLogin(Optional.ofNullable(null));
            userService.updateUser(secondGoogleUser.getId(), update, UserRole.ADMIN);
            fail("should throw ActionNotAllowedException");
        } catch (ActionNotAllowedException e) {
            assertThat(e.getMessage(), equalTo(Messages.DUPLICATE_IDENTITY_LOGIN));
        }

        String facebook = RandomStringUtils.randomAlphabetic(10);
        UserVO firstFacebookUser = new UserVO();
        firstFacebookUser.setLogin(RandomStringUtils.randomAlphabetic(10));
        firstFacebookUser.setFacebookLogin(facebook);
        firstFacebookUser.setStatus(UserStatus.ACTIVE);
        userService.createUser(firstFacebookUser, RandomStringUtils.randomAlphabetic(10));

        UserVO secondFacebookUser = new UserVO();
        secondFacebookUser.setLogin(RandomStringUtils.randomAlphabetic(10));
        secondFacebookUser.setFacebookLogin(RandomStringUtils.randomAlphabetic(10));
        secondFacebookUser.setStatus(UserStatus.ACTIVE);
        secondFacebookUser = userService.createUser(secondFacebookUser, RandomStringUtils.randomAlphabetic(10));
        try {
            UserUpdate update = new UserUpdate();
            update.setLogin(Optional.ofNullable(secondFacebookUser.getLogin()));
            update.setFacebookLogin(Optional.ofNullable(facebook));
            update.setGoogleLogin(Optional.ofNullable(null));
            update.setGithubLogin(Optional.ofNullable(null));
            userService.updateUser(secondFacebookUser.getId(), update, UserRole.ADMIN);
            fail("should throw ActionNotAllowedException");
        } catch (ActionNotAllowedException e) {
            assertThat(e.getMessage(), equalTo(Messages.DUPLICATE_IDENTITY_LOGIN));
        }

        String github = RandomStringUtils.randomAlphabetic(10);
        UserVO firstGithubUser = new UserVO();
        firstGithubUser.setLogin(RandomStringUtils.randomAlphabetic(10));
        firstGithubUser.setGithubLogin(github);
        firstGithubUser.setStatus(UserStatus.ACTIVE);
        userService.createUser(firstGithubUser, RandomStringUtils.randomAlphabetic(10));

        UserVO secondGithubUser = new UserVO();
        secondGithubUser.setLogin(RandomStringUtils.randomAlphabetic(10));
        secondGithubUser.setGithubLogin(RandomStringUtils.randomAlphabetic(10));
        secondGithubUser.setStatus(UserStatus.ACTIVE);
        userService.createUser(secondGithubUser, RandomStringUtils.randomAlphabetic(10));
        try {

            UserUpdate update = new UserUpdate();
            update.setLogin(Optional.ofNullable(secondGithubUser.getLogin()));
            update.setGithubLogin(Optional.ofNullable(github));
            update.setFacebookLogin(Optional.ofNullable(null));
            update.setGoogleLogin(Optional.ofNullable(null));
            userService.updateUser(secondGithubUser.getId(), update, UserRole.ADMIN);
            fail("should throw ActionNotAllowedException");
        } catch (ActionNotAllowedException e) {
            assertThat(e.getMessage(), equalTo(Messages.DUPLICATE_IDENTITY_LOGIN));
        }
    }

    @Test
    public void should_update_password_with_admin_role() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");

        UserUpdate update = new UserUpdate();
        update.setPassword(Optional.ofNullable("new_pass"));
        UserVO updatedUser = userService.updateUser(user.getId(), update, UserRole.ADMIN);
        assertThat(updatedUser, notNullValue());
        assertThat(updatedUser.getId(), equalTo(user.getId()));
        assertThat(updatedUser.getPasswordHash(), not(equalTo(user.getPasswordHash())));
        assertThat(updatedUser.getPasswordSalt(), not(equalTo(user.getPasswordSalt())));
    }

    @Test
    public void should_throw_ActionNotAllowedException_if_updating_password_with_client_role_without_old_password() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");

        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.OLD_PASSWORD_REQUIRED);

        UserUpdate update = new UserUpdate();
        update.setPassword(Optional.ofNullable("new_pass"));
        userService.updateUser(user.getId(), update, UserRole.CLIENT);
    }

    @Test
    public void should_update_password_with_client_role_if_old_password_provided() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");

        UserUpdate update = new UserUpdate();
        update.setPassword(Optional.ofNullable("new_pass"));
        update.setOldPassword(Optional.ofNullable("123"));
        UserVO updatedUser = userService.updateUser(user.getId(), update, UserRole.CLIENT);
        assertThat(updatedUser, notNullValue());
        assertThat(updatedUser.getId(), equalTo(user.getId()));
        assertThat(updatedUser.getPasswordHash(), not(equalTo(user.getPasswordHash())));
        assertThat(updatedUser.getPasswordSalt(), not(equalTo(user.getPasswordSalt())));
    }

    @Test
    public void should_throw_ActionNotAllowedException_if_updating_password_with_client_role_with_wrong_old_password() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");

        expectedException.expect(ActionNotAllowedException.class);
        expectedException.expectMessage(Messages.INCORRECT_CREDENTIALS);

        UserUpdate update = new UserUpdate();
        update.setPassword(Optional.ofNullable("new_pass"));
        update.setOldPassword(Optional.ofNullable("old"));
        userService.updateUser(user.getId(), update, UserRole.CLIENT);
    }

    @Test
    public void should_throw_IllegalParametersException_if_() throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");

        expectedException.expect(IllegalParametersException.class);
        expectedException.expectMessage(Messages.PASSWORD_REQUIRED);

        UserUpdate update = new UserUpdate();
        update.setPassword(Optional.ofNullable(null));
        userService.updateUser(user.getId(), update, UserRole.ADMIN);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_list_of_users_by_login() throws Exception {
        for (int i = 0; i < 10; i++) {
            UserVO user = new UserVO();
            user.setLogin(RandomStringUtils.randomAlphabetic(10));
            user.setStatus(UserStatus.ACTIVE);
            userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
        }
        UserVO testUser = new UserVO();
        testUser.setLogin(RandomStringUtils.randomAlphabetic(10));
        testUser.setStatus(UserStatus.ACTIVE);
        testUser = userService.createUser(testUser, RandomStringUtils.randomAlphabetic(10));
        handleListUserRequest();
        final UserVO finalTestUser = testUser;
        userService.list(testUser.getLogin(), null, null, null, null, null, 100, 0)
                .thenAccept(users -> {
                    assertThat(users, not(empty()));
                    assertThat(users, hasSize(1));
                    assertThat(users.stream().findFirst().get().getId(), equalTo(finalTestUser.getId()));
                }).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    public void should_return_list_of_users_by_login_pattern() throws Exception {
        for (int i = 0; i < 5; i++) {
            UserVO user = new UserVO();
            user.setLogin(RandomStringUtils.randomAlphabetic(10));
            user.setStatus(UserStatus.ACTIVE);
            userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
        }
        String prefix = RandomStringUtils.randomAlphabetic(10);
        for (int i = 0; i < 5; i++) {
            UserVO user = new UserVO();
            user.setLogin(prefix + RandomStringUtils.randomAlphabetic(10));
            user.setStatus(UserStatus.ACTIVE);
            userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
        }
        handleListUserRequest();
        userService.list(null, "%" + prefix + "%", null, null, null, null, 100, 0)
                .thenAccept(users -> {
                    assertThat(users, not(empty()));
                    assertThat(users, hasSize(5));
                    for (UserVO user : users) {
                        assertThat(user.getLogin(), startsWith(prefix));
                    }
                }).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_list_of_users_by_role() throws Exception {
        String prefix = RandomStringUtils.randomAlphabetic(5);
        for (int i = 0; i < 10; i++) {
            UserVO user = new UserVO();
            user.setLogin(prefix + RandomStringUtils.randomAlphabetic(10));
            user.setStatus(UserStatus.ACTIVE);
            user.setRole(UserRole.ADMIN);
            userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
        }
        for (int i = 0; i < 10; i++) {
            UserVO user = new UserVO();
            user.setLogin(prefix + RandomStringUtils.randomAlphabetic(10));
            user.setStatus(UserStatus.ACTIVE);
            user.setRole(UserRole.CLIENT);
            userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
        }
        handleListUserRequest();
        userService.list(null, "%" + prefix + "%", UserRole.CLIENT.getValue(), null, null, null, 100, 0)
                .thenAccept(users -> {
                    assertThat(users, not(empty()));
                    assertThat(users, hasSize(10));
                    for (UserVO user : users) {
                        assertThat(user.getLogin(), startsWith(prefix));
                        assertThat(user.getRole(), equalTo(UserRole.CLIENT));
                    }
                }).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_list_of_users_by_status() throws Exception {
        String prefix = RandomStringUtils.randomAlphabetic(5);
        for (int i = 0; i < 10; i++) {
            UserVO user = new UserVO();
            user.setLogin(prefix + RandomStringUtils.randomAlphabetic(10));
            user.setStatus(UserStatus.ACTIVE);
            userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
        }
        for (int i = 0; i < 10; i++) {
            UserVO user = new UserVO();
            user.setLogin(prefix + RandomStringUtils.randomAlphabetic(10));
            user.setStatus(UserStatus.LOCKED_OUT);
            userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
        }
        handleListUserRequest();
        userService.list(null, "%" + prefix + "%", null, UserStatus.LOCKED_OUT.getValue(), null, null, 100, 0)
                .thenAccept(users -> {
                    assertThat(users, not(empty()));
                    assertThat(users, hasSize(10));
                    for (UserVO user : users) {
                        assertThat(user.getLogin(), startsWith(prefix));
                        assertThat(user.getStatus(), equalTo(UserStatus.LOCKED_OUT));
                    }
                }).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_list_of_users_sorted() throws Exception {
        String suffix = RandomStringUtils.randomAlphabetic(5);
        List<String> prefixes = Arrays.asList("a", "b", "c", "d", "e");
        Collections.shuffle(prefixes);
        for (String prefix : prefixes) {
            UserVO user = new UserVO();
            user.setLogin(prefix + "_" + suffix);
            user.setStatus(UserStatus.ACTIVE);
            userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
        }
        handleListUserRequest();
        userService.list(null, "%" + suffix, null, null, "login", true, 100, 0)
                .thenAccept(users -> {
                    assertThat(users, not(empty()));
                    assertThat(users, hasSize(5));

                    assertThat(users.get(0).getLogin(), startsWith("a"));
                    assertThat(users.get(1).getLogin(), startsWith("b"));
                    assertThat(users.get(2).getLogin(), startsWith("c"));
                    assertThat(users.get(3).getLogin(), startsWith("d"));
                    assertThat(users.get(4).getLogin(), startsWith("e"));
                }).get(15, TimeUnit.SECONDS);

        userService.list(null, "%" + suffix, null, null, "login", false, 100, 0)
                .thenAccept(users -> {
                    assertThat(users, not(empty()));
                    assertThat(users, hasSize(5));

                    assertThat(users.get(0).getLogin(), startsWith("e"));
                    assertThat(users.get(1).getLogin(), startsWith("d"));
                    assertThat(users.get(2).getLogin(), startsWith("c"));
                    assertThat(users.get(3).getLogin(), startsWith("b"));
                    assertThat(users.get(4).getLogin(), startsWith("a"));
                }).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(2)).handle(argument.capture());
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_return_list_of_users_paginated() throws Exception {
        String prefix = RandomStringUtils.randomAlphabetic(5);
        List<Long> ids = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            UserVO user = new UserVO();
            user.setLogin(prefix + RandomStringUtils.randomAlphabetic(10));
            user.setStatus(UserStatus.ACTIVE);
            user = userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
            ids.add(user.getId());
        }
        handleListUserRequest();
        userService.list(null, "%" + prefix + "%", null, null, null, false, 20, 10)
                .thenAccept(users -> {
                    assertThat(users, not(empty()));
                    assertThat(users, hasSize(20));
                    List<Long> expectedIds = ids.stream().skip(10).limit(20).collect(Collectors.toList());
                    List<Long> returnedIds = users.stream().map(UserVO::getId).collect(Collectors.toList());
                    assertThat(returnedIds, equalTo(expectedIds));
                }).get(15, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    private void handleListUserRequest() {
        when(requestHandler.handle(any(Request.class))).thenAnswer(invocation -> {
            Request request = invocation.getArgumentAt(0, Request.class);
            ListUserRequest req = request.getBody().cast(ListUserRequest.class);
            final List<UserVO> users =
                    userDao.list(req.getLogin(), req.getLoginPattern(),
                            req.getRole(), req.getStatus(),
                            req.getSortField(), req.getSortOrderAsc(),
                            req.getTake(), req.getSkip());

            return Response.newBuilder()
                    .withBody(new ListUserResponse(users))
                    .buildSuccess();
        });
    }
}
