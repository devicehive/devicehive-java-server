package com.devicehive.service;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.GenericDAO;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.model.enums.UserStatus;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class UserServiceTest extends AbstractResourceTest {

    @Autowired
    private UserService userService;
    @Autowired
    private NetworkService networkService;
    @Autowired
    private GenericDAO genericDAO;
    @Autowired
    private ConfigurationService configurationService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void should_throw_NoSuchElementException_if_user_is_null_when_assign_network() throws Exception {
        Network network = new Network();
        network.setName("network");
        Network created = networkService.create(network);
        assertThat(created, notNullValue());
        assertThat(created.getId(), notNullValue());

        expectedException.expect(NoSuchElementException.class);
        expectedException.expectMessage(Messages.USER_NOT_FOUND);

        userService.assignNetwork(-1L, created.getId());
    }

    @Test
    public void should_throw_NoSuchElementException_if_network_is_null_when_assign_network() throws Exception {
        User user = new User();
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
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());

        Network network = new Network();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        Network first = networkService.create(network);
        assertThat(first, notNullValue());
        assertThat(first.getId(), notNullValue());

        userService.assignNetwork(user.getId(), first.getId());

        user = userService.findUserWithNetworks(user.getId());
        assertThat(user.getNetworks(), notNullValue());
        assertThat(user.getNetworks(), hasSize(1));
        assertThat(user.getNetworks(), hasItem(new CustomTypeSafeMatcher<Network>("expect network") {
            @Override
            protected boolean matchesSafely(Network item) {
                return first.getId().equals(item.getId()) && first.getName().equals(item.getName());
            }
        }));

        network = new Network();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        Network second = networkService.create(network);
        assertThat(second, notNullValue());
        assertThat(second.getId(), notNullValue());

        userService.assignNetwork(user.getId(), second.getId());

        user = userService.findUserWithNetworks(user.getId());
        assertThat(user.getNetworks(), notNullValue());
        assertThat(user.getNetworks(), hasSize(2));
        assertThat(user.getNetworks(),
                hasItems(
                        new CustomTypeSafeMatcher<Network>("expect network") {
                            @Override
                            protected boolean matchesSafely(Network item) {
                                return first.getId().equals(item.getId()) && first.getName().equals(item.getName());
                            }
                        },
                        new CustomTypeSafeMatcher<Network>("expect network") {
                            @Override
                            protected boolean matchesSafely(Network item) {
                                return second.getId().equals(item.getId()) && second.getName().equals(item.getName());
                            }
                        }
                ));
    }

    @Test
    public void should_unassign_network_from_user() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());

        Network network = new Network();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        Network first = networkService.create(network);
        assertThat(first, notNullValue());
        assertThat(first.getId(), notNullValue());
        userService.assignNetwork(user.getId(), first.getId());
        user = userService.findUserWithNetworks(user.getId());
        assertThat(user.getNetworks(), notNullValue());
        assertThat(user.getNetworks(), hasSize(1));

        network = new Network();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        Network second = networkService.create(network);
        assertThat(second, notNullValue());
        assertThat(second.getId(), notNullValue());
        userService.assignNetwork(user.getId(), second.getId());
        user = userService.findUserWithNetworks(user.getId());
        assertThat(user.getNetworks(), notNullValue());
        assertThat(user.getNetworks(), hasSize(2));

        userService.unassignNetwork(user.getId(), first.getId());

        user = userService.findUserWithNetworks(user.getId());
        assertThat(user.getNetworks(), hasSize(1));
        assertThat(user.getNetworks(), hasItem(new CustomTypeSafeMatcher<Network>("expect network") {
            @Override
            protected boolean matchesSafely(Network item) {
                return second.getId().equals(item.getId()) && second.getName().equals(item.getName());
            }

        }));

        userService.unassignNetwork(user.getId(), second.getId());
        user = userService.findUserWithNetworks(user.getId());
        assertThat(user.getNetworks(), is(empty()));
    }

    @Test
    public void should_throw_NoSuchElementException_if_user_is_null_when_unassign_network() throws Exception {
        Network network = new Network();
        network.setName(RandomStringUtils.randomAlphabetic(10));
        Network created = networkService.create(network);
        assertThat(created, notNullValue());
        assertThat(created.getId(), notNullValue());

        expectedException.expect(NoSuchElementException.class);
        expectedException.expectMessage(Messages.USER_NOT_FOUND);

        userService.unassignNetwork(-1, created.getId());
    }

    @Test
    public void should_do_nothing_if_network_is_null_when_unassign_user_from_network() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());

        userService.unassignNetwork(user.getId(), -1);
    }

    @Test
    public void should_return_null_if_user_does_not_exist_when_authenticate() throws Exception {
        User user = userService.authenticate(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10));
        assertThat(user, nullValue());
    }

    @Test
    public void should_authenticate_user_successfully() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));

        User authenticated = userService.authenticate(user.getLogin(), "123");
        assertThat(authenticated, notNullValue());
        assertThat(authenticated.getLoginAttempts(), equalTo(0));
        assertThat(authenticated.getLastLogin(), notNullValue());
    }

    @Test
    public void should_increase_login_attempts_if_user_failed_to_login() throws Exception {
        User newUser = new User();
        newUser.setLogin(RandomStringUtils.randomAlphabetic(10));
        newUser.setStatus(UserStatus.ACTIVE);
        User user = userService.createUser(newUser, "123");
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
            User updatedUser = genericDAO.find(User.class, user.getId());
            assertThat(updatedUser, notNullValue());
            assertThat(updatedUser.getLoginAttempts(), equalTo(attempt));
            assertThat(updatedUser.getStatus(), equalTo(UserStatus.ACTIVE));
            assertThat(updatedUser.getLastLogin(), nullValue());
        });
    }

    @Test
    public void should_lock_user_if_max_login_attempts_reached() throws Exception {
        User newUser = new User();
        newUser.setLogin(RandomStringUtils.randomAlphabetic(10));
        newUser.setStatus(UserStatus.ACTIVE);
        User user = userService.createUser(newUser, "123");
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
            User updatedUser = genericDAO.find(User.class, user.getId());
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
        User updatedUser = genericDAO.find(User.class, user.getId());
        assertThat(updatedUser, notNullValue());
        assertThat(updatedUser.getLoginAttempts(), equalTo(0));
        assertThat(updatedUser.getStatus(), equalTo(UserStatus.LOCKED_OUT));
        assertThat(updatedUser.getLastLogin(), nullValue());
    }

    @Test
    public void should_authenticate_user_after_not_successful_authentication() throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setStatus(UserStatus.ACTIVE);
        user = userService.createUser(user, "123");
        assertThat(user, notNullValue());
        assertThat(user.getId(), notNullValue());
        assertThat(user.getLogin(), notNullValue());
        assertThat(user.getPasswordHash(), notNullValue());
        assertThat(user.getPasswordSalt(), notNullValue());
        assertThat(user.getStatus(), equalTo(UserStatus.ACTIVE));

        User authenticated = userService.authenticate(user.getLogin(), "123");
        assertThat(authenticated, notNullValue());
        assertThat(authenticated.getLoginAttempts(), equalTo(0));
        assertThat(authenticated.getLastLogin(), notNullValue());

        try {
            userService.authenticate(user.getLogin(), "1234");
            fail("should throw login exception");
        } catch (ActionNotAllowedException e) {
            assertThat(e.getMessage(), equalTo(String.format(Messages.INCORRECT_CREDENTIALS, user.getLogin())));
        }
        User updatedUser = genericDAO.find(User.class, user.getId());
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
        User user = new User();
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

        User authenticated1 = userService.authenticate(user.getLogin(), "123");
        User authenticated2 = userService.authenticate(user.getLogin(), "123");

        assertThat(authenticated1.getId(), equalTo(authenticated2.getId()));
        assertThat(authenticated1.getLogin(), equalTo(authenticated2.getLogin()));
        assertThat(authenticated1.getLastLogin(), equalTo(authenticated2.getLastLogin()));
        assertThat(authenticated1.getLoginAttempts(), equalTo(authenticated2.getLoginAttempts()));
    }

    @Test
    public void should_modify_last_login_if_login_timeout_is_reached() throws Exception {
        User user = new User();
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
        User authenticated1 = userService.authenticate(user.getLogin(), "123");
        User authenticated2 = userService.authenticate(user.getLogin(), "123");

        assertThat(authenticated1.getId(), equalTo(authenticated2.getId()));
        assertThat(authenticated1.getLogin(), equalTo(authenticated2.getLogin()));
        assertThat(authenticated1.getLastLogin(), not(equalTo(authenticated2.getLastLogin())));
        assertTrue(authenticated1.getLastLogin().before(authenticated2.getLastLogin()));
        assertThat(authenticated1.getLoginAttempts(), equalTo(authenticated2.getLoginAttempts()));
    }
}
