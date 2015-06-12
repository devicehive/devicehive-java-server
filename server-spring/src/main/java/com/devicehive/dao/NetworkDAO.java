package com.devicehive.dao;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.dao.filter.AccessKeyBasedFilterForNetworks;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.devicehive.model.Network.Queries.Names.*;
import static com.devicehive.model.Network.Queries.Parameters.ID;
import static com.devicehive.model.Network.Queries.Parameters.NAME;

@Component
public class NetworkDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    @Transactional
    public Network createNetwork(Network network) {
        em.persist(network);
        return network;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Network getWithDevicesAndDeviceClasses(@NotNull long id) {
        TypedQuery<Network> query = em.createNamedQuery(GET_WITH_DEVICES_AND_DEVICE_CLASSES, Network.class);
        query.setParameter(ID, id);
        CacheHelper.cacheable(query);
        List<Network> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Network> getNetworkList(@NotNull User user,
                                        Set<AccessKeyPermission> permissions,
                                        List<Long> networkIds) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Network> criteria = criteriaBuilder.createQuery(Network.class);
        Root<Network> from = criteria.from(Network.class);
        List<Predicate> predicates = new ArrayList<>();
        if (!user.isAdmin()) {
            predicates.add(from.join(Network.USERS_ASSOCIATION).get(User.ID_COLUMN).in(user.getId()));
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
                    extraPredicates.add(criteriaBuilder.and(filter.toArray(new Predicate[filter.size()])));
                }
                predicates.add(criteriaBuilder.or(extraPredicates.toArray(new Predicate[extraPredicates.size()])));
            }
        }

        if (networkIds != null && !networkIds.isEmpty()) {
            predicates.add(from.get(Network.ID_COLUMN).in(networkIds));
        }

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));
        TypedQuery<Network> query = em.createQuery(criteria);
        CacheHelper.cacheable(query);
        query.setHint(CacheHelper.STORE_MODE, CacheStoreMode.REFRESH);
        return query.getResultList();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Network getById(@NotNull long id) {
        return em.find(Network.class, id);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Network findByName(@NotNull String name) {
        TypedQuery<Network> query = em.createNamedQuery(FIND_BY_NAME, Network.class);
        query.setParameter(NAME, name);
        CacheHelper.cacheable(query);
        List<Network> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Transactional
    public Network merge(@NotNull Network n) {
        return em.merge(n);
    }

    @Transactional
    public boolean delete(@NotNull Long id) {
        Query query = em.createNamedQuery(DELETE_BY_ID);
        query.setParameter(ID, id);
        return query.executeUpdate() != 0;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<Network> list(String name,
                              String namePattern,
                              String sortField,
                              Boolean sortOrderAsc,
                              Integer take,
                              Integer skip,
                              HivePrincipal principal) {

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Network> criteria = criteriaBuilder.createQuery(Network.class);
        Root<Network> from = criteria.from(Network.class);

        List<Predicate> predicates = new ArrayList<>();
        if (namePattern != null) {
            predicates.add(criteriaBuilder.like(from.<String>get(Network.NAME_COLUMN), namePattern));
        } else {
            if (name != null) {
                predicates.add(criteriaBuilder.equal(from.get(Network.NAME_COLUMN), name));
            }
        }

        appendPrincipalPredicates(predicates, principal, from);

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
        }
        resultQuery.setMaxResults(take);
        CacheHelper.cacheable(resultQuery);

        return resultQuery.getResultList();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Network getByIdWithUsers(@NotNull long id) {
        TypedQuery<Network> query = em.createNamedQuery(FIND_WITH_USERS, Network.class);
        query.setParameter(ID, id);
        CacheHelper.cacheable(query);
        List<Network> networks = query.getResultList();
        return networks.isEmpty() ? null : networks.get(0);
    }

    private void appendPrincipalPredicates(List<Predicate> predicates, HivePrincipal principal, Root<Network> from) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        if (principal != null) {
            User user = principal.getUser();
            if (user == null && principal.getKey() != null) {
                user = principal.getKey().getUser();
            }
            if (user != null && !user.isAdmin()) {
                Path<User> path = from.join(Network.USERS_ASSOCIATION);
                predicates.add(path.in(user));
            }
            if (principal.getDevice() != null) {
                throw new HiveException("Can not get access to networks", 403);
            }
            if (principal.getKey() != null) {

                List<Predicate> extraPredicates = new ArrayList<>();
                for (AccessKeyBasedFilterForDevices extraFilter : AccessKeyBasedFilterForDevices
                    .createExtraFilters(principal.getKey().getPermissions())) {
                    List<Predicate> filter = new ArrayList<>();
                    if (extraFilter.getNetworkIds() != null) {
                        filter.add(from.get("id").in(extraFilter.getNetworkIds()));
                    }
                    extraPredicates.add(criteriaBuilder.and(filter.toArray(new Predicate[filter.size()])));
                }
                predicates.add(criteriaBuilder.or(extraPredicates.toArray(new Predicate[extraPredicates.size()])));
            }
        }
    }

    @Transactional
    public Network updateNetwork(@NotNull Network network) {
        em.merge(network);
        return network;
    }
}