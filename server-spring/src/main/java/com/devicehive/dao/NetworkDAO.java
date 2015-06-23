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
    public Network getByIdWithUsers(@NotNull long id) {
        TypedQuery<Network> query = em.createNamedQuery(FIND_WITH_USERS, Network.class);
        query.setParameter(ID, id);
        CacheHelper.cacheable(query);
        List<Network> networks = query.getResultList();
        return networks.isEmpty() ? null : networks.get(0);
    }

}
