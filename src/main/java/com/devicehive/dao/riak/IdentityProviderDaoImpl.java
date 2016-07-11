package com.devicehive.dao.riak;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.IdentityProviderDao;
import com.devicehive.model.IdentityProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Profile({"riak"})
@Repository
public class IdentityProviderDaoImpl extends GenericDaoImpl implements IdentityProviderDao {
    @Override
    public IdentityProvider getByName(@NotNull String name) {
        return createNamedQuery(IdentityProvider.class, "IdentityProvider.getByName", Optional.of(CacheConfig.refresh()))
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Override
    public boolean deleteById(@NotNull Long id) {
        return createNamedQuery("IdentityProvider.deleteById", Optional.of(CacheConfig.bypass()))
                .setParameter("id", id)
                .executeUpdate() > 0;
    }

    @Override
    public IdentityProvider find(@NotNull Long id) {
        return find(IdentityProvider.class, id);
    }

    @Override
    public IdentityProvider merge(IdentityProvider existing) {
        return super.merge(existing);
    }

    @Override
    public void persist(IdentityProvider identityProvider) {
        super.persist(identityProvider);
    }
}
