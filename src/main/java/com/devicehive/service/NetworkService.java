package com.devicehive.service;

import com.devicehive.configuration.Constants;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.Network;
import com.devicehive.service.interceptors.ValidationInterceptor;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Interceptors(ValidationInterceptor.class)
@Stateless
public class NetworkService {

    @PersistenceContext(unitName = Constants.PERSISTENCE_UNIT)
    private EntityManager em;

    public Network createNetwork(Network network) {
        em.persist(network);
        return network;
    }

    public Network createOrVeriryNetwork(Network network) {
        Network stored;
        if (network.getId() != null) {
            stored = em.find(Network.class, network.getId());
        } else {
            TypedQuery<Network> query = em.createNamedQuery("Network.findByName", Network.class);
            query.setParameter("name", network.getName());
            List<Network> result = query.getResultList();
            stored = result.isEmpty() ? null : result.get(0);
        }
        if (stored != null) {
            if (stored.getKey() != null) {
                if (!stored.getKey().equals(network.getKey())) {
                    throw new HiveException("Wrong network key!");
                }
            }
        } else {
            stored = createNetwork(network);
        }
        assert (stored != null);
        return stored;
    }

}
