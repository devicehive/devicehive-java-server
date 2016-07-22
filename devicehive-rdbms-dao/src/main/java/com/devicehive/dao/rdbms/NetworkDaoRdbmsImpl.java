package com.devicehive.dao.rdbms;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.CriteriaHelper;
import com.devicehive.dao.NetworkDao;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Profile({"rdbms"})
@Repository
public class NetworkDaoRdbmsImpl extends RdbmsGenericDao implements NetworkDao {
    @Override
    public List<Network> findByName(String name) {
        return createNamedQuery(Network.class, "Network.findByName", Optional.of(CacheConfig.get()))
                .setParameter("name", name)
                .getResultList();
    }

    @Override
    public List<Network> getNetworksByIdsAndUsers(Long idForFiltering, Set<Long> networkId, Set<Long> permittedNetworks) {
        TypedQuery<Network> query = createNamedQuery(Network.class, "Network.getNetworksByIdsAndUsers",
                Optional.of(CacheConfig.bypass()))
                .setParameter("userId", idForFiltering)
                .setParameter("networkIds", networkId)
                .setParameter("permittedNetworks", permittedNetworks);
        return query.getResultList();
    }

    @Override
    public int deleteById(long id) {
        return createNamedQuery("Network.deleteById", Optional.<CacheConfig>empty())
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public void persist(Network newNetwork) {
        super.persist(newNetwork);
    }

    @Override
    public Network find(@NotNull Long networkId) {
        return find(Network.class, networkId);
    }

    @Override
    public Network merge(Network existing) {
        return super.merge(existing);
    }

    @Override
    public void assignToNetwork(Network network, User user) {
        assert network != null && network.getId() != null;
        assert user != null && user.getId() != null;
        network.getUsers().add(user);
        merge(network);
    }

    @Override
    public List<Network> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take, Integer skip, Optional<HivePrincipal> principal) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Network> criteria = cb.createQuery(Network.class);
        Root<Network> from = criteria.from(Network.class);

        Predicate[] nameAndPrincipalPredicates = CriteriaHelper.networkListPredicates(cb, from, ofNullable(name), ofNullable(namePattern), principal);
        criteria.where(nameAndPrincipalPredicates);

        CriteriaHelper.order(cb, criteria, from, ofNullable(sortField), sortOrderAsc);

        TypedQuery<Network> query = createQuery(criteria);
        cacheQuery(query, of(CacheConfig.refresh()));
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);
        return query.getResultList();
    }

    @Override
    public Optional<Network> findFirstByName(String name) {
        return findByName(name).stream().findFirst();
    }

    @Override
    public Optional<Network> findWithUsers(@NotNull long networkId) {
        return createNamedQuery(Network.class, "Network.findWithUsers", Optional.of(CacheConfig.refresh()))
                .setParameter("id", networkId)
                .getResultList()
                .stream().findFirst();
    }
}
