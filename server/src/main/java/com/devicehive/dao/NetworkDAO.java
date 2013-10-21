package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.dao.filter.AccessKeyBasedFilterForNetworks;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.Device;
import com.devicehive.model.Network;
import com.devicehive.model.User;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Stateless
public class NetworkDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public Network createNetwork(Network network) {
        em.persist(network);
        return network;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Network getWithDevicesAndDeviceClasses(@NotNull long id) {
        TypedQuery<Network> query = em.createNamedQuery("Network.getWithDevicesAndDeviceClasses", Network.class);
        query.setParameter("id", id);
        List<Network> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public List<Network> getNetworkList(@NotNull User user,
                                        Set<AccessKeyPermission> permissions,
                                        List<Long> networkIds) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Network> criteria = criteriaBuilder.createQuery(Network.class);
        Root<Network> from = criteria.from(Network.class);
        List<Predicate> predicates = new ArrayList<>();
        if (!user.isAdmin()) {
            predicates.add(from.join("users").get("id").in(user.getId()));
        }
        if (permissions != null) {
            Collection<AccessKeyBasedFilterForNetworks> extraFilters = AccessKeyBasedFilterForNetworks
                    .createExtraFilters(permissions);

            if (extraFilters != null) {
                List<Predicate> extraPredicates = new ArrayList<>();
                for (AccessKeyBasedFilterForNetworks extraFilter : extraFilters) {
                    List<Predicate> filter = new ArrayList<>();
                    if (extraFilter.getNetworkIds() != null) {
                        filter.add(from.get("id").in(extraFilter.getNetworkIds()));
                    }
                    extraPredicates.add(criteriaBuilder.and(filter.toArray(new Predicate[0])));
                }
                predicates.add(criteriaBuilder.or(extraPredicates.toArray(new Predicate[0])));
            }
        }

        if (networkIds != null && !networkIds.isEmpty()) {
            predicates.add(from.get("id").in(networkIds));
        }

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        TypedQuery<Network> query = em.createQuery(criteria);
        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Network getById(@NotNull long id) {
        return em.find(Network.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Network findByName(@NotNull String name) {
        TypedQuery<Network> query = em.createNamedQuery("Network.findByName", Network.class);
        query.setParameter("name", name);
        CacheHelper.cacheable(query);
        List<Network> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public Network merge(@NotNull Network n) {
        return em.merge(n);
    }

    public boolean delete(@NotNull Long id) {
        Query query = em.createNamedQuery("Network.deleteById");
        query.setParameter("id", id);
        return query.executeUpdate() != 0;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Network> list(String name,
                              String namePattern,
                              String sortField,
                              Boolean sortOrderAsc,
                              Integer take,
                              Integer skip,
                              User user,
                              Collection<AccessKeyBasedFilterForDevices> extraFilters) {

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Network> criteria = criteriaBuilder.createQuery(Network.class);
        Root<Network> from = criteria.from(Network.class);

        List<Predicate> predicates = new ArrayList<>();
        if (namePattern != null) {
            predicates.add(criteriaBuilder.like(from.<String>get("name"), namePattern));
        } else {
            if (name != null) {
                predicates.add(criteriaBuilder.equal(from.get("name"), name));
            }
        }

        if (!user.isAdmin()) {
            Path<User> path = from.join("users");
            predicates.add(path.in(user));
        }

        if (extraFilters != null) {
            List<Predicate> extraPredicates = new ArrayList<>();
            for (AccessKeyBasedFilterForDevices extraFilter : extraFilters) {
                List<Predicate> filter = new ArrayList<>();
                if (extraFilter.getNetworkIds() != null) {
                    filter.add(from.get("id").in(extraFilter.getNetworkIds()));
                }
                extraPredicates.add(criteriaBuilder.and(filter.toArray(new Predicate[0])));
            }
            predicates.add(criteriaBuilder.or(extraPredicates.toArray(new Predicate[0])));
        }


        criteria.where(predicates.toArray(new Predicate[predicates.size()]));

        if (sortField != null) {
            if (sortOrderAsc) {
                criteria.orderBy(criteriaBuilder.asc(from.get(sortField)));
            } else {
                criteria.orderBy(criteriaBuilder.desc(from.get(sortField)));
            }
        }

        TypedQuery<Network> resultQuery = em.createQuery(criteria);
        if (skip != null) {
            resultQuery.setFirstResult(skip);
        }
        if (take == null) {
            take = Constants.DEFAULT_TAKE;
            resultQuery.setMaxResults(take);
        }

        return resultQuery.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Network getByIdWithUsers(@NotNull long id) {
        TypedQuery<Network> query = em.createNamedQuery("Network.findWithUsers", Network.class);
        query.setParameter("id", id);
        List<Network> networks = query.getResultList();
        return networks.isEmpty() ? null : networks.get(0);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Network> getByNameOrId(Long networkId, String networkName) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Network> networkCriteria = criteriaBuilder.createQuery(Network.class);
        Root fromNetwork = networkCriteria.from(Network.class);
        List<Predicate> networkPredicates = new ArrayList<>();
        if (networkId != null) {
            networkPredicates.add(criteriaBuilder.equal(fromNetwork.get("id"), networkId));
        }
        if (networkName != null) {
            networkPredicates.add(criteriaBuilder.equal(fromNetwork.get("name"), networkName));
        }
        networkCriteria.where(networkPredicates.toArray(new Predicate[networkPredicates.size()]));
        TypedQuery<Network> networksQuery = em.createQuery(networkCriteria);
        return networksQuery.getResultList();
    }

}
