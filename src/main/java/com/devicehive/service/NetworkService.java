package com.devicehive.service;

import com.devicehive.dao.NetworkDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.Network;

import javax.inject.Inject;
import javax.transaction.Transactional;


public class NetworkService {

    @Inject
    private NetworkDAO networkDAO;

    @Transactional
    public Network getNetwork(Network networkFromMessage) {
        Network network;
        if (networkFromMessage.getId() != null) {
            network = networkDAO.findById(networkFromMessage.getId());
        } else {
            network = networkDAO.findByName(networkFromMessage.getName());
        }
        if (network == null) {
            networkDAO.addNetwork(networkFromMessage);
            network = networkFromMessage;

        } else {
            network = updateNetworkIfRequired(network, networkFromMessage);
        }
        return network;
    }

    private Network updateNetworkIfRequired(Network networkfromDB, Network networkFromMessage) {
        if (networkfromDB.getKey() != null) {
            if (!networkfromDB.getKey().equals(networkFromMessage.getKey())) {
                throw new HiveException("Wrong network key!");
            }
        }

        boolean updateNetwork = false;
        if (networkFromMessage.getName() != null && !networkFromMessage.getName().equals
                (networkfromDB.getName())) {
            networkfromDB.setName(networkFromMessage.getName());
            updateNetwork = true;
        }
        if (networkFromMessage.getDescription() != null && !networkFromMessage.getDescription().equals
                (networkfromDB.getDescription())) {
            networkfromDB.setDescription(networkFromMessage.getDescription());
            updateNetwork = true;
        }
        if (updateNetwork) {
            networkDAO.updateNetwork(networkfromDB);
        }
        return networkfromDB;
    }

}
