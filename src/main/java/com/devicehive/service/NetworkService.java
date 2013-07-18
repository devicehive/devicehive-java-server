package com.devicehive.service;

import com.devicehive.dao.NetworkDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.Network;

import javax.ejb.Stateless;
import javax.inject.Inject;

@Stateless
public class NetworkService {

    @Inject
    private NetworkDAO networkDAO;

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
