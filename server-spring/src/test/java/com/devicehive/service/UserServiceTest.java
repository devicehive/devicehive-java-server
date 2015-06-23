package com.devicehive.service;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.configuration.Messages;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.CustomMatcher;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class UserServiceTest extends AbstractResourceTest {

    @Autowired
    private UserService userService;
    @Autowired
    private NetworkService networkService;

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
}
