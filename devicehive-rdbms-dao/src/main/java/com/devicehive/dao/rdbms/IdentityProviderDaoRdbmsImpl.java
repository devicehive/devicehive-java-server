package com.devicehive.dao.rdbms;

import com.devicehive.dao.IdentityProviderDao;
import com.devicehive.model.IdentityProvider;
import com.devicehive.vo.IdentityProviderVO;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Repository
public class IdentityProviderDaoRdbmsImpl extends RdbmsGenericDao implements IdentityProviderDao {
    @Override
    public IdentityProviderVO getByName(@NotNull String name) {
        IdentityProvider identityProvider = createNamedQuery(IdentityProvider.class, "IdentityProvider.getByName", Optional.of(CacheConfig.refresh()))
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst().orElse(null);
        return IdentityProvider.convertToVO(identityProvider);
    }

    @Override
    public boolean deleteById(@NotNull String id) {
        return createNamedQuery("IdentityProvider.deleteByName", Optional.of(CacheConfig.bypass()))
                .setParameter("name", id)
                .executeUpdate() > 0;
    }

    @Override
    public IdentityProviderVO merge(IdentityProviderVO existing) {
        IdentityProvider identityProvider = IdentityProvider.convertToEntity(existing);
        IdentityProvider merge = super.merge(identityProvider);
        return IdentityProvider.convertToVO(merge);
    }

    @Override
    public void persist(IdentityProviderVO identityProvider) {
        IdentityProvider newEntity = IdentityProvider.convertToEntity(identityProvider);
        super.persist(newEntity);
    }
}
