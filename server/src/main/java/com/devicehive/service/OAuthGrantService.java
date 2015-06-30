package com.devicehive.service;

import com.devicehive.configuration.Messages;
import com.devicehive.dao.OAuthGrantDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AccessKey;
import com.devicehive.model.OAuthClient;
import com.devicehive.model.OAuthGrant;
import com.devicehive.model.User;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.enums.Type;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.model.updates.OAuthGrantUpdate;
import com.devicehive.service.time.TimestampService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Component
public class OAuthGrantService {

    @Autowired
    private OAuthGrantDAO grantDAO;
    @Autowired
    private AccessKeyService accessKeyService;
    @Autowired
    private OAuthClientService clientService;
    @Autowired
    private UserService userService;
    @Autowired
    private TimestampService timestampService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OAuthGrant get(@NotNull User user, @NotNull Long grantId) {
        if (user.isAdmin()) {
            return grantDAO.get(grantId);
        }
        return grantDAO.get(user, grantId);
    }

    @Transactional
    public OAuthGrant save(@NotNull OAuthGrant grant, @NotNull User user) {
        validate(grant);
        OAuthClient client = clientService.getByOAuthID(grant.getClient().getOauthId());
        grant.setClient(client);
        if (grant.getAccessType() == null) {
            grant.setAccessType(AccessType.ONLINE);
        }
        Date now = timestampService.getTimestamp();
        grant.setTimestamp(now);
        AccessKey key = accessKeyService.createAccessKeyFromOAuthGrant(grant, user, now);
        grant.setAccessKey(key);
        grant.setUser(user);
        if (grant.getType().equals(Type.CODE)) {
            grant.setAuthCode(UUID.randomUUID().toString());
        }

        grantDAO.insert(grant);
        return grant;
    }

    @Transactional
    public boolean delete(@NotNull User user, @NotNull Long grantId) {
        if (user.isAdmin()) {
            OAuthGrant existing = grantDAO.get(grantId);
            if (existing == null) {
                return true;
            }
            accessKeyService.delete(null, existing.getAccessKey().getId());
            return grantDAO.delete(grantId);
        }
        OAuthGrant existing = grantDAO.get(user, grantId);
        if (existing == null) {
            return true;
        }
        accessKeyService.delete(user.getId(), existing.getAccessKey().getId());
        return grantDAO.delete(user, grantId);
    }

    @Transactional
    public OAuthGrant update(@NotNull User user, @NotNull Long grantId, OAuthGrantUpdate grantToUpdate) {
        OAuthGrant existing = get(user, grantId);
        if (existing == null) {
            return null;
        }
        OAuthClient client = existing.getClient();
        if (grantToUpdate.getClient() != null) {
            OAuthClient clientFromGrant = grantToUpdate.getClient().getValue();
            if (clientFromGrant == null) {
                throw new HiveException(Messages.CLIENT_IS_NULL);
            }
            if (clientFromGrant.getId() != null) {
                client = clientService.get(clientFromGrant.getId());
            } else if (clientFromGrant.getName() != null) {
                client = clientService.getByName(clientFromGrant.getName());
            }

        }
        existing.setClient(client);
        if (grantToUpdate.getAccessType() != null) {
            existing.setAccessType(grantToUpdate.getAccessType().getValue());
        }
        if (grantToUpdate.getType() != null) {
            existing.setType(grantToUpdate.getType().getValue());
        }
        if (grantToUpdate.getNetworkIds() != null) {
            existing.setNetworkIds(grantToUpdate.getNetworkIds().getValue());
        }
        if (grantToUpdate.getRedirectUri() != null) {
            existing.setRedirectUri(grantToUpdate.getRedirectUri().getValue());
        }
        if (grantToUpdate.getScope() != null) {
            existing.setScope(grantToUpdate.getScope().getValue());
        }
        Date now = timestampService.getTimestamp();
        existing.setTimestamp(now);
        AccessKey key = accessKeyService.updateAccessKeyFromOAuthGrant(existing, user, now);
        existing.setAccessKey(key);
        if (existing.getAuthCode() != null) {
            existing.setAuthCode(UUID.randomUUID().toString());
        }
        return existing;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<OAuthGrant> list(@NotNull User user,
                                 Date start,
                                 Date end,
                                 String clientOAuthId,
                                 Integer type,
                                 String scope,
                                 String redirectUri,
                                 Integer accessType,
                                 String sortField,
                                 Boolean sortOrder,
                                 Integer take,
                                 Integer skip) {
        return grantDAO.get(user, start, end, clientOAuthId, type, scope, redirectUri, accessType, sortField,
                            sortOrder, take, skip);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public OAuthGrant get(@NotNull String authCode, @NotNull String clientOAuthID) {
        return grantDAO.getByCodeAndOauthID(authCode, clientOAuthID);
    }


    public AccessKey accessTokenRequestForCodeType(@NotNull String code,
                                                   String redirectUri,
                                                   @NotNull String clientId) {
        OAuthGrant grant = get(code, clientId);
        if (grant == null || !grant.getType().equals(Type.CODE)) {
            throw new HiveException(Messages.INVALID_AUTH_CODE, SC_UNAUTHORIZED);
        }
        if (redirectUri != null && !grant.getRedirectUri().equals(redirectUri)) {
            throw new HiveException(Messages.INVALID_URI, SC_UNAUTHORIZED);
        }
        if (grant.getTimestamp().getTime() - timestampService.getTimestamp().getTime() > 600_000) {
            throw new HiveException(Messages.EXPIRED_GRANT, SC_UNAUTHORIZED);
        }
        invalidate(grant);
        return grant.getAccessKey();
    }

    @Transactional
    public AccessKey accessTokenRequestForPasswordType(@NotNull String scope,
                                                       @NotNull String login,
                                                       @NotNull String password,
                                                       OAuthClient client) {
        User user = userService.authenticate(login, password);
        if (user == null || !user.getStatus().equals(UserStatus.ACTIVE)) {
            throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, SC_UNAUTHORIZED);
        }
        user.setLastLogin(timestampService.getTimestamp());
        List<OAuthGrant> found = grantDAO.get(user, null, null, client.getOauthId(), Type.PASSWORD.ordinal(), scope,
                                              null, null, null, null, null, null);
        OAuthGrant grant = found.isEmpty() ? null : found.get(0);
        Date now = timestampService.getTimestamp();
        if (grant == null) {
            grant = new OAuthGrant();
            grant.setClient(client);
            grant.setUser(user);
            grant.setRedirectUri(client.getRedirectUri());
            grant.setTimestamp(now);
            grant.setScope(scope);
            grant.setAccessType(AccessType.ONLINE);
            grant.setType(Type.PASSWORD);
            AccessKey key = accessKeyService.createAccessKeyFromOAuthGrant(grant, user, now);
            grant.setAccessKey(key);
            grantDAO.insert(grant);
        } else {
            AccessKey key = accessKeyService.updateAccessKeyFromOAuthGrant(grant, user, now);
            grant.setAccessKey(key);
        }
        return grant.getAccessKey();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    private void invalidate(OAuthGrant grant) {
        grant.setAuthCode(null);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    private void validate(OAuthGrant grant) {
        List<String> violations = new ArrayList<>();
        if (grant.getClient() == null) {
            violations.add(Messages.CLIENT_REQUIRED);
        }
        if (grant.getType() == null) {
            violations.add(Messages.TYPE_REQUIRED);
        }
        if (grant.getType() != null && grant.getType().equals(Type.PASSWORD)) {
            violations.add(Messages.INVALID_GRANT_TYPE);
        }
        if (grant.getRedirectUri() == null) {
            violations.add(Messages.REDIRECT_URI_REQUIRED);
        }
        if (grant.getScope() == null) {
            violations.add(Messages.SCOPE_REQUIRED);
        }
        if (!violations.isEmpty()) {
            throw new HiveException(String.format(Messages.VALIDATION_FAILED, StringUtils.join(violations, "; ")),
                                    Response.Status.BAD_REQUEST.getStatusCode());
        }
    }
}
