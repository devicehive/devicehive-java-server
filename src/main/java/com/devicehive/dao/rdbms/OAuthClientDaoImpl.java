package com.devicehive.dao.rdbms;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.OAuthClientDao;
import com.devicehive.model.OAuthClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Profile({"rdbms"})
@Repository
public class OAuthClientDaoImpl extends GenericDaoImpl implements OAuthClientDao {

    @Override
    public int deleteById(Long id) {
        return createNamedQuery("OAuthClient.deleteById", Optional.<CacheConfig>empty())
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public OAuthClient getByOAuthId(String oauthId) {
        return createNamedQuery(OAuthClient.class, "OAuthClient.getByOAuthId", Optional.of(CacheConfig.refresh()))
                .setParameter("oauthId", oauthId)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Override
    public OAuthClient getByName(String name) {
        return createNamedQuery(OAuthClient.class, "OAuthClient.getByName", Optional.of(CacheConfig.refresh()))
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Override
    public OAuthClient getByOAuthIdAndSecret(String id, String secret) {
        return createNamedQuery(OAuthClient.class, "OAuthClient.getByOAuthIdAndSecret", Optional.of(CacheConfig.get()))
                .setParameter("oauthId", id)
                .setParameter("secret", secret)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Override
    public OAuthClient find(Long id) {
        return find(OAuthClient.class, id);
    }

    @Override
    public void persist(OAuthClient oAuthClient) {
        super.persist(oAuthClient);
    }

    @Override
    public OAuthClient merge(OAuthClient existing) {
        return super.merge(existing);
    }
}
