package com.devicehive.dao.rdbms;

import com.devicehive.dao.OAuthClientDao;
import com.devicehive.model.OAuthClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Profile({"rdbms"})
@Repository
public class OAuthClientDaoRdbmsImpl extends RdbmsGenericDao implements OAuthClientDao {

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

    @Override
    public List<OAuthClient> get(String name,
                                 String namePattern,
                                 String domain,
                                 String oauthId,
                                 String sortField,
                                 Boolean sortOrderAsc,
                                 Integer take,
                                 Integer skip) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<OAuthClient> cq = cb.createQuery(OAuthClient.class);
        Root<OAuthClient> from = cq.from(OAuthClient.class);

        Predicate[] predicates = CriteriaHelper.oAuthClientListPredicates(cb, from, ofNullable(name), ofNullable(namePattern), ofNullable(domain), ofNullable(oauthId));
        cq.where(predicates);
        CriteriaHelper.order(cb, cq, from, ofNullable(sortField), Boolean.TRUE.equals(sortOrderAsc));

        TypedQuery<OAuthClient> query = createQuery(cq);
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);

        return query.getResultList();
    }
}
