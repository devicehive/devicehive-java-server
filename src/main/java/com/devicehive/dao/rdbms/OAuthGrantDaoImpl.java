package com.devicehive.dao.rdbms;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.OAuthGrantDao;
import com.devicehive.model.OAuthGrant;
import com.devicehive.model.User;
import org.springframework.stereotype.Repository;

import static java.util.Optional.of;

@Repository
public class OAuthGrantDaoImpl extends GenericDaoImpl implements OAuthGrantDao {

    public OAuthGrant getByIdAndUser(User user, Long grantId) {
        return createNamedQuery(OAuthGrant.class, "OAuthGrant.getByIdAndUser",
                of(CacheConfig.refresh()))
                .setParameter("grantId", grantId)
                .setParameter("user", user)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    public OAuthGrant getById(Long grantId) {
        return createNamedQuery(OAuthGrant.class, "OAuthGrant.getById",
                of(CacheConfig.refresh()))
                .setParameter("grantId", grantId)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    public int deleteByUserAndId(User user, Long grantId) {
        return createNamedQuery("OAuthGrant.deleteByUserAndId", of(CacheConfig.refresh()))
                .setParameter("grantId", grantId)
                .setParameter("user", user)
                .executeUpdate();
    }

    public OAuthGrant getByCodeAndOAuthID(String authCode, String clientOAuthID) {
        return createNamedQuery(OAuthGrant.class, "OAuthGrant.getByCodeAndOAuthID", of(CacheConfig.refresh()))
                .setParameter("authCode", authCode)
                .setParameter("oauthId", clientOAuthID)
                .getResultList()
                .stream().findFirst().orElse(null);
    }
}
