package com.devicehive.service;

import com.devicehive.configuration.Messages;
import com.devicehive.dao.OAuthGrantDao;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.enums.Type;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.model.updates.OAuthGrantUpdate;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.AccessKeyVO;
import com.devicehive.vo.OAuthClientVO;
import com.devicehive.vo.OAuthGrantVO;
import com.devicehive.vo.UserVO;
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
    private OAuthGrantDao oAuthGrantDao;
    @Autowired
    private AccessKeyService accessKeyService;
    @Autowired
    private OAuthClientService clientService;
    @Autowired
    private UserService userService;
    @Autowired
    private TimestampService timestampService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OAuthGrantVO get(@NotNull UserVO user, @NotNull Long grantId) {
        if (user.isAdmin()) {
            return oAuthGrantDao.getById(grantId);
        } else {
            return oAuthGrantDao.getByIdAndUser(user, grantId);
        }
    }

    @Transactional
    public OAuthGrantVO save(@NotNull OAuthGrantVO grant, @NotNull UserVO user) {
        validate(grant);
        OAuthClientVO client = clientService.getByOAuthID(grant.getClient().getOauthId());
        grant.setClient(client);
        if (grant.getAccessType() == null) {
            grant.setAccessType(AccessType.ONLINE);
        }
        Date now = timestampService.getDate();
        grant.setTimestamp(now);
        AccessKeyVO key = accessKeyService.createAccessKeyFromOAuthGrant(grant, user, now);
        grant.setAccessKey(key);
        grant.setUser(user);
        if (grant.getType().equals(Type.CODE)) {
            grant.setAuthCode(UUID.randomUUID().toString());
        }

        oAuthGrantDao.persist(grant);
        return grant;
    }

    @Transactional
    public boolean delete(@NotNull UserVO user, @NotNull Long grantId) {
        OAuthGrantVO existing;
        if (user.isAdmin()) {
            existing = oAuthGrantDao.getById(grantId);
        } else {
            existing = oAuthGrantDao.getByIdAndUser(user, grantId);
        }

        if (existing == null) {
            return true;
        }

        accessKeyService.delete(user.isAdmin() ? null : user.getId(), existing.getAccessKey().getId());

        int result = oAuthGrantDao.deleteByUserAndId(user, grantId);
        return result > 0;
    }

    @Transactional
    public OAuthGrantVO update(@NotNull UserVO user, @NotNull Long grantId, OAuthGrantUpdate grantToUpdate) {
        OAuthGrantVO existing = get(user, grantId);
        if (existing == null) {
            return null;
        }
        OAuthClientVO client = existing.getClient();
        if (grantToUpdate.getClient() != null) {
            OAuthClientVO clientFromGrant = grantToUpdate.getClient().orElse(null);
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
            existing.setAccessType(grantToUpdate.getAccessType().orElse(null));
        }
        if (grantToUpdate.getType() != null) {
            existing.setType(grantToUpdate.getType().orElse(null));
        }
        if (grantToUpdate.getNetworkIds() != null) {
            existing.setNetworkIds(grantToUpdate.getNetworkIds().orElse(null));
        }
        if (grantToUpdate.getRedirectUri() != null) {
            existing.setRedirectUri(grantToUpdate.getRedirectUri().orElse(null));
        }
        if (grantToUpdate.getScope() != null) {
            existing.setScope(grantToUpdate.getScope().orElse(null));
        }
        Date now = timestampService.getDate();
        existing.setTimestamp(now);
        AccessKeyVO key = accessKeyService.updateAccessKeyFromOAuthGrant(existing, user, now);
        existing.setAccessKey(key);
        if (existing.getAuthCode() != null) {
            existing.setAuthCode(UUID.randomUUID().toString());
        }
        if (existing.getUser() == null) {
            existing.setUser(user);
        }
        oAuthGrantDao.merge(existing);
        return existing;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<OAuthGrantVO> list(@NotNull UserVO user,
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

        return oAuthGrantDao.list(user,
                start,
                end,
                clientOAuthId,
                type,
                scope,
                redirectUri,
                accessType,
                sortField,
                sortOrder,
                take,
                skip);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public OAuthGrantVO get(@NotNull String authCode, @NotNull String clientOAuthID) {
        return oAuthGrantDao.getByCodeAndOAuthID(authCode, clientOAuthID);
    }


    public AccessKeyVO accessTokenRequestForCodeType(@NotNull String code,
                                                   String redirectUri,
                                                   @NotNull String clientId) {
        OAuthGrantVO grant = get(code, clientId);
        if (grant == null || !grant.getType().equals(Type.CODE)) {
            throw new HiveException(Messages.INVALID_AUTH_CODE, SC_UNAUTHORIZED);
        }
        if (redirectUri != null && !grant.getRedirectUri().equals(redirectUri)) {
            throw new HiveException(Messages.INVALID_URI, SC_UNAUTHORIZED);
        }
        if (grant.getTimestamp().getTime() - timestampService.getTimestamp() > 600_000) {
            throw new HiveException(Messages.EXPIRED_GRANT, SC_UNAUTHORIZED);
        }
        grant.setAuthCode(null);
        return grant.getAccessKey();
    }

    @Transactional
    public AccessKeyVO accessTokenRequestForPasswordType(@NotNull String scope,
                                                       @NotNull String login,
                                                       @NotNull String password,
                                                       OAuthClientVO client) {
        UserVO user = userService.authenticate(login, password);
        if (user == null || !user.getStatus().equals(UserStatus.ACTIVE)) {
            throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, SC_UNAUTHORIZED);
        }
        user.setLastLogin(timestampService.getDate());
        List<OAuthGrantVO> found = list(user, null, null, client.getOauthId(), Type.PASSWORD.ordinal(), scope,
                null, null, null, null, null, null);
        OAuthGrantVO grant = found.isEmpty() ? null : found.get(0);
        Date now = timestampService.getDate();
        if (grant == null) {
            grant = new OAuthGrantVO();
            grant.setClient(client);
            grant.setUser(user);
            grant.setRedirectUri(client.getRedirectUri());
            grant.setTimestamp(now);
            grant.setScope(scope);
            grant.setAccessType(AccessType.ONLINE);
            grant.setType(Type.PASSWORD);
            AccessKeyVO key = accessKeyService.createAccessKeyFromOAuthGrant(grant, user, now);
            grant.setAccessKey(key);
            oAuthGrantDao.persist(grant);
        } else {
            AccessKeyVO key = accessKeyService.updateAccessKeyFromOAuthGrant(grant, user, now);
            grant.setAccessKey(key);
        }
        return grant.getAccessKey();
    }

    private void validate(OAuthGrantVO grant) {
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
