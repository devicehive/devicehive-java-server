package com.devicehive.dao.rdbms;

import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.OAuthGrantDao;
import com.devicehive.model.OAuthGrant;
import com.devicehive.model.User;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

import static com.devicehive.dao.CriteriaHelper.oAuthGrantsListPredicates;
import static com.devicehive.dao.CriteriaHelper.order;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Profile({"rdbms"})
@Repository
public class OAuthGrantDaoImpl extends GenericDaoImpl implements OAuthGrantDao {

    @Override
    public OAuthGrant getByIdAndUser(User user, Long grantId) {
        return createNamedQuery(OAuthGrant.class, "OAuthGrant.getByIdAndUser",
                of(CacheConfig.refresh()))
                .setParameter("grantId", grantId)
                .setParameter("user", user)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Override
    public OAuthGrant getById(Long grantId) {
        return createNamedQuery(OAuthGrant.class, "OAuthGrant.getById",
                of(CacheConfig.refresh()))
                .setParameter("grantId", grantId)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Override
    public int deleteByUserAndId(User user, Long grantId) {
        return createNamedQuery("OAuthGrant.deleteByUserAndId", of(CacheConfig.refresh()))
                .setParameter("grantId", grantId)
                .setParameter("user", user)
                .executeUpdate();
    }

    @Override
    public OAuthGrant getByCodeAndOAuthID(String authCode, String clientOAuthID) {
        return createNamedQuery(OAuthGrant.class, "OAuthGrant.getByCodeAndOAuthID", of(CacheConfig.refresh()))
                .setParameter("authCode", authCode)
                .setParameter("oauthId", clientOAuthID)
                .getResultList()
                .stream().findFirst().orElse(null);
    }

    @Override
    public OAuthGrant find(Long id) {
        return find(OAuthGrant.class, id);
    }

    @Override
    public void persist(OAuthGrant oAuthGrant) {
        super.persist(oAuthGrant);
    }

    @Override
    public OAuthGrant merge(OAuthGrant existing) {
        return super.merge(existing);
    }

    @Override
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
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<OAuthGrant> cq = cb.createQuery(OAuthGrant.class);
        Root<OAuthGrant> from = cq.from(OAuthGrant.class);
        from.fetch("accessKey", JoinType.LEFT).fetch("permissions");
        from.fetch("client");

        Predicate[] predicates = oAuthGrantsListPredicates(cb, from, user, ofNullable(start), ofNullable(end), ofNullable(clientOAuthId), ofNullable(type), ofNullable(scope),
                ofNullable(redirectUri), ofNullable(accessType));
        cq.where(predicates);
        order(cb, cq, from, ofNullable(sortField), Boolean.TRUE.equals(sortOrder));

        TypedQuery<OAuthGrant> query = createQuery(cq);
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);

        return query.getResultList();
    }
}
