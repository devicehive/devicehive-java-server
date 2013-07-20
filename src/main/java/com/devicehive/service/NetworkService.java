package com.devicehive.service;

import com.devicehive.configuration.Constants;
import com.devicehive.dao.NetworkDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.Network;
import com.devicehive.service.interceptors.ValidationInterceptor;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class NetworkService {

    @Inject
    private NetworkDAO networkDAO;

    @Inject
    private NetworkDAO networkDAO;

    public Network getById(long id) {
        return networkDAO.getById(id);
    }

    public void delete(long id) {
        networkDAO.delete(id);
    }

    public Network insert(Network n) {
        if (n.getName() == null) {
            throw new HiveException("Name must be provided");
        }
        return networkDAO.insert(n);
    }


    public List<Network> list(String name, String namePattern,
                              String sortField, boolean sortOrder,
                              Integer take, Integer skip) {
        return networkDAO.list(name, namePattern, sortField, sortOrder, take, skip);

    }


    public Network createOrVeriryNetwork(Network network) {
        Network stored;
        if (network.getId() != null) {
            stored = networkDAO.getById(network.getId());
        } else {
            stored = networkDAO.findByName(network.getName());
        }
        if (stored != null) {
            if (stored.getKey() != null) {
                if (!stored.getKey().equals(network.getKey())) {
                    throw new HiveException("Wrong network key!");
                }
            }
        } else {
            stored = networkDAO.createNetwork(network);
        }
        assert (stored != null);
        return stored;
    }
}
