package com.devicehive.dao;

import com.devicehive.configuration.Constants;
import com.devicehive.model.Network;
import com.devicehive.service.interceptors.ValidationInterceptor;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nikolay Loboda <madlooser@gmail.com>
 * @since 7/18/13 2:27 AM
 */
@Stateless
@Interceptors(ValidationInterceptor.class)
public class NetworkDAO {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    private static final Logger logger = LoggerFactory.getLogger(NetworkDAO.class);

    private static final Integer DEFAULT_TAKE = 1000; //TODO set parameter

    public Network getById(@NotNull long id) {
        return em.find(Network.class,id);
    }

    public void delete(@NotNull long id) {
        Network n = em.find(Network.class,id);
        em.remove(n);
    }

    public Network insert(Network n) {
        return  em.merge(n);
    }


    public List<Network> list(String name, String namePattern,
                              String sortField, boolean sortOrderAsc,
                              Integer take, Integer skip) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Network> criteria = criteriaBuilder.createQuery(Network.class);
        Root from = criteria.from(Network.class);
        List<Predicate> predicates = new ArrayList<>();

        if (namePattern != null) {
            predicates.add(criteriaBuilder.like(from.get("name"), namePattern));
        } else {
            if (name != null) {
                predicates.add(criteriaBuilder.equal(from.get("name"), name));
            }
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
            take = DEFAULT_TAKE;
            resultQuery.setMaxResults(take);
        }

        return resultQuery.getResultList();
    }

    public Network getByIdWithUsers(@NotNull long id) {
        Network result = em.find(Network.class,id);
        Hibernate.initialize(result.getUsers());
        return result;
    }

}
