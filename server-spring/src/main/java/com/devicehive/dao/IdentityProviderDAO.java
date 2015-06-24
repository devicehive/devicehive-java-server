package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.IdentityProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;
import java.util.List;

import static com.devicehive.model.IdentityProvider.Queries.Names.DELETE_BY_ID;
import static com.devicehive.model.IdentityProvider.Queries.Names.GET_BY_NAME;
import static com.devicehive.model.IdentityProvider.Queries.Parameters.ID;
import static com.devicehive.model.IdentityProvider.Queries.Parameters.NAME;


/**
 * Created by tmatvienko on 11/17/14.
 */
@Component
public class IdentityProviderDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public IdentityProvider insert(IdentityProvider newIdentityProvider) {
        em.persist(newIdentityProvider);
        return newIdentityProvider;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public IdentityProvider get(Long id) {
        return em.find(IdentityProvider.class, id);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public IdentityProvider get(String name) {
        TypedQuery<IdentityProvider> query = em.createNamedQuery(GET_BY_NAME,
                IdentityProvider.class);
        query.setParameter(NAME, name);
        CacheHelper.cacheable(query);
        List<IdentityProvider> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Transactional
    public IdentityProvider update(@NotNull IdentityProvider identityProvider) {
        em.merge(identityProvider);
        return identityProvider;
    }

    @Transactional
    public boolean delete(@NotNull Long id) {
        Query query = em.createNamedQuery(DELETE_BY_ID);
        query.setParameter(ID, id);
        return query.executeUpdate() != 0;
    }
}
