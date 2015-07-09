package com.devicehive.service;

import com.devicehive.auth.AccessKeyAction;
import com.devicehive.base.AbstractResourceTest;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.OAuthClient;
import com.devicehive.model.OAuthGrant;
import com.devicehive.model.User;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.enums.Type;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.updates.OAuthGrantUpdate;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class OAuthGrantServiceTest extends AbstractResourceTest {

    @Autowired
    private OAuthGrantService grantService;
    @Autowired
    private OAuthClientService oAuthClientService;
    @Autowired
    private UserService userService;

    @Test
    public void should_save_grant() throws Exception {
        OAuthClient client = createClient();
        User user = createUser(UserRole.CLIENT);
        OAuthGrant grant = new OAuthGrant();
        grant.setClient(client);
        grant.setType(Type.CODE);
        grant.setRedirectUri(RandomStringUtils.randomAlphabetic(10));
        grant.setScope(AccessKeyAction.GET_DEVICE.getValue());
        grant = grantService.save(grant, user);
        assertThat(grant, notNullValue());
        assertThat(grant.getId(), notNullValue());
        assertThat(grant.getAccessKey(), notNullValue());
        assertThat(grant.getTimestamp(), notNullValue());
        assertThat(grant.getAccessType(), notNullValue());
        assertThat(grant.getClient(), notNullValue());
        assertThat(grant.getType(), notNullValue());
        assertThat(grant.getRedirectUri(), notNullValue());
        assertThat(grant.getScope(), notNullValue());
    }

    private OAuthClient createClient() {
        OAuthClient client = new OAuthClient();
        client.setName(RandomStringUtils.randomAlphabetic(10));
        client.setOauthId(RandomStringUtils.randomAlphabetic(10));
        client.setOauthSecret(RandomStringUtils.randomAlphabetic(10));
        client.setDomain(RandomStringUtils.randomAlphabetic(10));
        client.setRedirectUri(RandomStringUtils.randomAlphabetic(10));
        return oAuthClientService.insert(client);
    }

    public User createUser(UserRole role) throws Exception {
        User user = new User();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(role);
        return userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
    }

    public OAuthGrant createGrant(OAuthClient client, User user) {
        OAuthGrant grant = new OAuthGrant();
        grant.setClient(client);
        grant.setType(Type.CODE);
        grant.setRedirectUri(RandomStringUtils.randomAlphabetic(10));
        grant.setScope(AccessKeyAction.GET_DEVICE.getValue());
        return grantService.save(grant, user);
    }

    @Test
    public void should_get_grant_by_user() throws Exception {
        OAuthClient client = createClient();
        User user = createUser(UserRole.CLIENT);
        OAuthGrant grant = createGrant(client, user);

        OAuthGrant returned = grantService.get(user, grant.getId());
        assertThat(returned, notNullValue());
        assertThat(returned.getId(), equalTo(grant.getId()));
    }

    @Test
    public void should_not_get_grant_by_not_owner() throws Exception {
        OAuthClient client = createClient();
        User user = createUser(UserRole.CLIENT);
        OAuthGrant grant = createGrant(client, user);

        User anotherUser = createUser(UserRole.CLIENT);
        OAuthGrant returned = grantService.get(anotherUser, grant.getId());
        assertThat(returned, nullValue());
    }

    @Test
    public void should_get_grant_by_admin() throws Exception {
        OAuthClient client = createClient();
        User user = createUser(UserRole.CLIENT);
        OAuthGrant grant = createGrant(client, user);

        User admin = createUser(UserRole.ADMIN);
        OAuthGrant returned = grantService.get(admin, grant.getId());
        assertThat(returned, notNullValue());
        assertThat(returned.getId(), equalTo(grant.getId()));
    }

    @Test
    public void should_update_oauth_grant() throws Exception {
        OAuthClient client = createClient();
        User user = createUser(UserRole.CLIENT);
        OAuthGrant grant = createGrant(client, user);

        OAuthClient newClient = createClient();
        OAuthGrantUpdate update = new OAuthGrantUpdate();
        update.setClient(new NullableWrapper<>(newClient));
        update.setAccessType(new NullableWrapper<>(AccessType.OFFLINE));
        update.setType(new NullableWrapper<>(Type.TOKEN));
        update.setRedirectUri(new NullableWrapper<>(RandomStringUtils.randomAlphabetic(10)));
        update.setScope(new NullableWrapper<>(AccessKeyAction.MANAGE_ACCESS_KEY.getValue()));
        grantService.update(user, grant.getId(), update);

        OAuthGrant updatedGrant = grantService.get(user, grant.getId());
        assertThat(updatedGrant, notNullValue());
        assertThat(updatedGrant.getAccessType(), equalTo(AccessType.OFFLINE));
        assertThat(updatedGrant.getRedirectUri(), equalTo(update.getRedirectUri().getValue()));
        assertThat(updatedGrant.getScope(), equalTo(AccessKeyAction.MANAGE_ACCESS_KEY.getValue()));
        assertThat(updatedGrant.getClient(), notNullValue());
        assertThat(updatedGrant.getClient().getId(), not(equalTo(client.getId())));
        assertThat(updatedGrant.getAccessKey(), notNullValue());
    }

    @Test
    public void should_return_grant_by_auth_code_and_client_auth_id() throws Exception {
        OAuthClient client = new OAuthClient();
        client.setName(RandomStringUtils.randomAlphabetic(10));
        client.setOauthId(RandomStringUtils.randomAlphabetic(10));
        client.setOauthSecret(RandomStringUtils.randomAlphabetic(10));
        client.setDomain(RandomStringUtils.randomAlphabetic(10));
        client.setRedirectUri(RandomStringUtils.randomAlphabetic(10));
        client = oAuthClientService.insert(client);

        User user = createUser(UserRole.CLIENT);

        OAuthGrant grant = new OAuthGrant();
        grant.setClient(client);
        grant.setType(Type.CODE);
        grant.setRedirectUri(RandomStringUtils.randomAlphabetic(10));
        grant.setScope(AccessKeyAction.GET_DEVICE.getValue());
        grant = grantService.save(grant, user);

        OAuthGrant returned = grantService.get(grant.getAuthCode(), client.getOauthId());
        assertThat(returned, notNullValue());
        assertThat(returned.getId(), equalTo(grant.getId()));
    }
}
