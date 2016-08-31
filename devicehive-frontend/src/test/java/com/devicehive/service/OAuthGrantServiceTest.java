package com.devicehive.service;

import com.devicehive.auth.AccessKeyAction;
import com.devicehive.base.AbstractResourceTest;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.enums.Type;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.updates.OAuthGrantUpdate;
import com.devicehive.vo.OAuthClientVO;
import com.devicehive.vo.OAuthGrantVO;
import com.devicehive.vo.UserVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

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
        OAuthClientVO client = createClient();
        UserVO user = createUser(UserRole.CLIENT);
        OAuthGrantVO grant = new OAuthGrantVO();
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

    private OAuthClientVO createClient() {
        OAuthClientVO client = new OAuthClientVO();
        client.setName(RandomStringUtils.randomAlphabetic(10));
        client.setOauthId(RandomStringUtils.randomAlphabetic(10));
        client.setOauthSecret(RandomStringUtils.randomAlphabetic(10));
        client.setDomain(RandomStringUtils.randomAlphabetic(10));
        client.setRedirectUri(RandomStringUtils.randomAlphabetic(10));
        return oAuthClientService.insert(client);
    }

    public UserVO createUser(UserRole role) throws Exception {
        UserVO user = new UserVO();
        user.setLogin(RandomStringUtils.randomAlphabetic(10));
        user.setRole(role);
        return userService.createUser(user, RandomStringUtils.randomAlphabetic(10));
    }

    public OAuthGrantVO createGrant(OAuthClientVO client, UserVO user) {
        OAuthGrantVO grant = new OAuthGrantVO();
        grant.setClient(client);
        grant.setType(Type.CODE);
        grant.setRedirectUri(RandomStringUtils.randomAlphabetic(10));
        grant.setScope(AccessKeyAction.GET_DEVICE.getValue());
        return grantService.save(grant, user);
    }

    @Test
    public void should_get_grant_by_user() throws Exception {
        OAuthClientVO client = createClient();
        UserVO user = createUser(UserRole.CLIENT);
        OAuthGrantVO grant = createGrant(client, user);

        OAuthGrantVO returned = grantService.get(user, grant.getId());
        assertThat(returned, notNullValue());
        assertThat(returned.getId(), equalTo(grant.getId()));
    }

    @Test
    public void should_not_get_grant_by_not_owner() throws Exception {
        OAuthClientVO client = createClient();
        UserVO user = createUser(UserRole.CLIENT);
        OAuthGrantVO grant = createGrant(client, user);

        UserVO anotherUser = createUser(UserRole.CLIENT);
        OAuthGrantVO returned = grantService.get(anotherUser, grant.getId());
        assertThat(returned, nullValue());
    }

    @Test
    public void should_get_grant_by_admin() throws Exception {
        OAuthClientVO client = createClient();
        UserVO user = createUser(UserRole.CLIENT);
        OAuthGrantVO grant = createGrant(client, user);

        UserVO admin = createUser(UserRole.ADMIN);
        OAuthGrantVO returned = grantService.get(admin, grant.getId());
        assertThat(returned, notNullValue());
        assertThat(returned.getId(), equalTo(grant.getId()));
    }

    @Test
    public void should_update_oauth_grant() throws Exception {
        OAuthClientVO client = createClient();
        UserVO user = createUser(UserRole.CLIENT);
        OAuthGrantVO grant = createGrant(client, user);

        OAuthClientVO newClient = createClient();
        OAuthGrantUpdate update = new OAuthGrantUpdate();
        update.setClient(Optional.ofNullable(newClient));
        update.setAccessType(Optional.ofNullable(AccessType.OFFLINE));
        update.setType(Optional.ofNullable(Type.TOKEN));
        update.setRedirectUri(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        update.setScope(Optional.ofNullable(AccessKeyAction.MANAGE_ACCESS_KEY.getValue()));
        grantService.update(user, grant.getId(), update);

        OAuthGrantVO updatedGrant = grantService.get(user, grant.getId());
        assertThat(updatedGrant, notNullValue());
        assertThat(updatedGrant.getAccessType(), equalTo(AccessType.OFFLINE));
        assertThat(updatedGrant.getRedirectUri(), equalTo(update.getRedirectUri().orElse(null)));
        assertThat(updatedGrant.getScope(), equalTo(AccessKeyAction.MANAGE_ACCESS_KEY.getValue()));
        assertThat(updatedGrant.getClient(), notNullValue());
        assertThat(updatedGrant.getClient().getId(), not(equalTo(client.getId())));
        assertThat(updatedGrant.getAccessKey(), notNullValue());
    }

    @Test
    public void should_return_grant_by_auth_code_and_client_auth_id() throws Exception {
        OAuthClientVO client = new OAuthClientVO();
        client.setName(RandomStringUtils.randomAlphabetic(10));
        client.setOauthId(RandomStringUtils.randomAlphabetic(10));
        client.setOauthSecret(RandomStringUtils.randomAlphabetic(10));
        client.setDomain(RandomStringUtils.randomAlphabetic(10));
        client.setRedirectUri(RandomStringUtils.randomAlphabetic(10));
        client = oAuthClientService.insert(client);

        UserVO user = createUser(UserRole.CLIENT);

        OAuthGrantVO grant = new OAuthGrantVO();
        grant.setClient(client);
        grant.setType(Type.CODE);
        grant.setRedirectUri(RandomStringUtils.randomAlphabetic(10));
        grant.setScope(AccessKeyAction.GET_DEVICE.getValue());
        grant = grantService.save(grant, user);

        OAuthGrantVO returned = grantService.get(grant.getAuthCode(), client.getOauthId());
        assertThat(returned, notNullValue());
        assertThat(returned.getId(), equalTo(grant.getId()));
    }
}
