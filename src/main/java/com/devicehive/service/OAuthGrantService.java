package com.devicehive.service;

import com.devicehive.configuration.Messages;
import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.GenericDAO;
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

import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.*;

import static com.devicehive.dao.CriteriaHelper.oAuthGrantsListPredicates;
import static com.devicehive.dao.CriteriaHelper.order;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Component
public class OAuthGrantService {

    @Autowired
    private GenericDAO genericDAO;
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
        String queryName = user.isAdmin() ? "OAuthGrant.getById" : "OAuthGrant.getByIdAndUser";
        TypedQuery<OAuthGrant> query = genericDAO.createNamedQuery(OAuthGrant.class, queryName, of(CacheConfig.refresh()))
                .setParameter("grantId", grantId);
        if (!user.isAdmin()) {
            query.setParameter("user", user);
        }
        return query.getResultList()
                .stream().findFirst().orElse(null);
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

        genericDAO.persist(grant);
        return grant;
    }

    @Transactional
    public boolean delete(@NotNull User user, @NotNull Long grantId) {
        String queryName = user.isAdmin() ? "OAuthGrant.getById" : "OAuthGrant.getByIdAndUser";
        TypedQuery<OAuthGrant> query = genericDAO.createNamedQuery(OAuthGrant.class, queryName, of(CacheConfig.get()))
                .setParameter("grantId", grantId);
        if (!user.isAdmin()) {
            query.setParameter("user", user);
        }
        Optional<OAuthGrant> existingOpt = query.getResultList().stream().findFirst();
        if (!existingOpt.isPresent()) {
            return true;
        }
        OAuthGrant existing = existingOpt.get();
        accessKeyService.delete(user.isAdmin() ? null : user.getId(), existing.getAccessKey().getId());

        int result = genericDAO.createNamedQuery("OAuthGrant.deleteByUserAndId", of(CacheConfig.refresh()))
                .setParameter("grantId", grantId)
                .setParameter("user", user)
                .executeUpdate();
        return result > 0;
    }

    @Transactional
    public OAuthGrant update(@NotNull User user, @NotNull Long grantId, OAuthGrantUpdate grantToUpdate) {
        OAuthGrant existing = get(user, grantId);
        if (existing == null) {
            return null;
        }
        OAuthClient client = existing.getClient();
        if (grantToUpdate.getClient() != null) {
            OAuthClient clientFromGrant = grantToUpdate.getClient().orElse(null);
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

        CriteriaBuilder cb = genericDAO.criteriaBuilder();
        CriteriaQuery<OAuthGrant> cq = cb.createQuery(OAuthGrant.class);
        Root<OAuthGrant> from = cq.from(OAuthGrant.class);
        from.fetch("accessKey", JoinType.LEFT).fetch("permissions");
        from.fetch("client");

        Predicate[] predicates = oAuthGrantsListPredicates(cb, from, user, ofNullable(start), ofNullable(end), ofNullable(clientOAuthId), ofNullable(type), ofNullable(scope),
                ofNullable(redirectUri), ofNullable(accessType));
        cq.where(predicates);
        order(cb, cq, from, ofNullable(sortField), Boolean.TRUE.equals(sortOrder));

        TypedQuery<OAuthGrant> query = genericDAO.createQuery(cq);
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);

        return query.getResultList();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public OAuthGrant get(@NotNull String authCode, @NotNull String clientOAuthID) {
        return genericDAO.createNamedQuery(OAuthGrant.class, "OAuthGrant.getByCodeAndOAuthID", of(CacheConfig.refresh()))
                .setParameter("authCode", authCode)
                .setParameter("oauthId", clientOAuthID)
                .getResultList()
                .stream().findFirst().orElse(null);
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
        grant.setAuthCode(null);
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
        List<OAuthGrant> found = list(user, null, null, client.getOauthId(), Type.PASSWORD.ordinal(), scope,
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
            genericDAO.persist(grant);
        } else {
            AccessKey key = accessKeyService.updateAccessKeyFromOAuthGrant(grant, user, now);
            grant.setAccessKey(key);
        }
        return grant.getAccessKey();
    }

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
