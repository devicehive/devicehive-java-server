package com.devicehive.dao.rdbms;

import com.devicehive.dao.OAuthClientDao;
import com.devicehive.model.OAuthClient;
import com.devicehive.vo.OAuthClientVO;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@Repository
public class OAuthClientDaoRdbmsImpl extends RdbmsGenericDao implements OAuthClientDao {

    @Override
    public int deleteById(Long id) {
        return createNamedQuery("OAuthClient.deleteById", Optional.<CacheConfig>empty())
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public OAuthClientVO getByOAuthId(String oauthId) {
        return OAuthClient.convert(createNamedQuery(OAuthClient.class, "OAuthClient.getByOAuthId", Optional.of(CacheConfig.refresh()))
                .setParameter("oauthId", oauthId)
                .getResultList()
                .stream().findFirst().orElse(null));
    }

    @Override
    public OAuthClientVO getByName(String name) {
        return OAuthClient.convert(createNamedQuery(OAuthClient.class, "OAuthClient.getByName", Optional.of(CacheConfig.refresh()))
                .setParameter("name", name)
                .getResultList()
                .stream().findFirst().orElse(null));
    }

    @Override
    public OAuthClientVO getByOAuthIdAndSecret(String id, String secret) {
        return OAuthClient.convert(createNamedQuery(OAuthClient.class, "OAuthClient.getByOAuthIdAndSecret", Optional.of(CacheConfig.get()))
                .setParameter("oauthId", id)
                .setParameter("secret", secret)
                .getResultList()
                .stream().findFirst().orElse(null));
    }

    @Override
    public OAuthClientVO find(Long id) {
        return OAuthClient.convert(find(OAuthClient.class, id));
    }

    @Override
    public void persist(OAuthClientVO oAuthClient) {
        OAuthClient client = OAuthClient.convert(oAuthClient);
        super.persist(client);
        oAuthClient.setId(client.getId());
    }

    @Override
    public OAuthClientVO merge(OAuthClientVO existing) {
        return OAuthClient.convert(super.merge(OAuthClient.convert(existing)));
    }

    @Override
    public List<OAuthClientVO> list(String name,
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
        Stream<OAuthClientVO> objectStream = query.getResultList().stream().map(OAuthClient::convert);
        return objectStream.collect(Collectors.toList());
    }
}
