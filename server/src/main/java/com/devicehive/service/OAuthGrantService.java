package com.devicehive.service;

import com.devicehive.dao.OAuthGrantDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.updates.OAuthGrantUpdate;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Stateless
public class OAuthGrantService {

    @EJB
    private OAuthGrantDAO grantDAO;
    private AccessKeyService accessKeyService;
    private OAuthClientService clientService;
    @EJB
    private TimestampService timestampService;

    @EJB
    public void setAccessKeyService(AccessKeyService accessKeyService) {
        this.accessKeyService = accessKeyService;
    }

    @EJB
    public void setClientService(OAuthClientService clientService) {
        this.clientService = clientService;
    }

    public OAuthGrant get(@NotNull User user, @NotNull Long grantId) {
        if (user.isAdmin()) {
            return grantDAO.get(grantId);
        }
        return grantDAO.get(user, grantId);
    }

    public OAuthGrant save(@NotNull OAuthGrant grant, @NotNull User user) {
        validate(grant);
        OAuthClient client = clientService.getByOAuthID(grant.getClient().getOauthId());
        grant.setClient(client);
        Timestamp now = timestampService.getTimestamp();
        grant.setTimestamp(now);
        AccessKey key = accessKeyService.createAccessKeyFromOAuthGrant(grant, user, now);
        grant.setAccessKey(key);
        grant.setUser(user);
        if (grant.getType().equals(Type.CODE)) {
            grant.setAuthCode(UUID.randomUUID().toString());
        }
        grant.setAccessType(AccessType.ONLINE);
        grantDAO.insert(grant);
        return grant;
    }

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

    public OAuthGrant update(@NotNull User user, @NotNull Long grantId, OAuthGrantUpdate grantToUpdate) {
        OAuthGrant existing = get(user, grantId);
        if (existing == null) {
            return null;
        }
        OAuthClient client = existing.getClient();
        if (grantToUpdate.getClient() != null) {
            client = grantToUpdate.getClient().getValue() == null
                    ? null
                    : grantToUpdate.getClient().getValue().convertTo();
        }
        existing.setClient(client);
        if (grantToUpdate.getAccessType() != null)
            existing.setAccessType(grantToUpdate.getAccessType().getValue());
        if (grantToUpdate.getType() != null)
            existing.setType(grantToUpdate.getType().getValue());
        if (grantToUpdate.getNetworkIds() != null)
            existing.setNetworkIds(grantToUpdate.getNetworkIds().getValue());
        if (grantToUpdate.getRedirectUri() != null)
            existing.setRedirectUri(grantToUpdate.getRedirectUri().getValue());
        if (grantToUpdate.getScope() != null){
            existing.setScope(grantToUpdate.getScope().getValue());
        }
        Timestamp now = timestampService.getTimestamp();
        existing.setTimestamp(now);
        AccessKey key = accessKeyService.updateAccessKeyFromOAuthGrant(existing, user, now);
        existing.setAccessKey(key);
        if (existing.getAuthCode() != null)
            existing.setAuthCode(UUID.randomUUID().toString());
        return existing;
    }

    public List<OAuthGrant> list(@NotNull User user,
                                 Timestamp start,
                                 Timestamp end,
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

    private void validate(OAuthGrant grant) {
        List<String> violations = new ArrayList<>();
        if (grant.getClient() == null) {
            violations.add("client field is required");
        }
        if (grant.getType() == null) {
            violations.add("type field is required");
        }
        if (grant.getType() != null && grant.getType().equals(Type.PASSWORD)) {
            violations.add("Unexpected type: password");
        }
        if (grant.getRedirectUri() == null) {
            violations.add("redirect URI field is required");
        }
        if (grant.getScope() == null) {
            violations.add("scope fieald is required");
        }
        if (!violations.isEmpty()) {
            throw new HiveException("Validation failed with following violations: "
                    + StringUtils.join(violations, "; "), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }
}
