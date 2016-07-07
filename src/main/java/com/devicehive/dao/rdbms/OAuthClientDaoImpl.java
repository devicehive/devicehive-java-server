package com.devicehive.dao.rdbms;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.OAuthClientDao;
import com.devicehive.model.OAuthClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OAuthClientDaoImpl extends GenericDaoImpl implements OAuthClientDao {

    public int deleteById(Long id) {
        return createNamedQuery("OAuthClient.deleteById", Optional.<CacheConfig>empty())
                .setParameter("id", id)
                .executeUpdate();
    }

    public OAuthClient getByOAuthId(String oauthId) {
        return createNamedQuery(OAuthClient.class, "OAuthClient.getByOAuthId", Optional.of(CacheConfig.refresh()))
                .setParameter("oauthId", oauthId)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    public OAuthClient getByName(String name) {
        return createNamedQuery(OAuthClient.class, "OAuthClient.getByName", Optional.of(CacheConfig.refresh()))
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    public OAuthClient getByOAuthIdAndSecret(String id, String secret) {
        return createNamedQuery(OAuthClient.class, "OAuthClient.getByOAuthIdAndSecret", Optional.of(CacheConfig.get()))
                .setParameter("oauthId", id)
                .setParameter("secret", secret)
                .getResultList()
                .stream().findFirst().orElse(null);
    }
}
