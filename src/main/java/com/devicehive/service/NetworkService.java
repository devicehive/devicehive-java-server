package com.devicehive.service;

import com.devicehive.dao.NetworkDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.Network;

import javax.inject.Inject;
import javax.transaction.Transactional;


public class NetworkService {

    @Inject
    private NetworkDAO networkDAO;

    public Network createOrUpdateNetworkAndGetIt(Network networkFromMessage) {
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
            if (network.getKey() != null) {
                if (!network.getKey().equals(networkFromMessage.getKey())) {
                    throw new HiveException("Wrong network key!");
                }
            }
            network = updateNetworkIfRequired(network, networkFromMessage);
        }
        return network;
    }

    @Transactional
    private Network updateNetworkIfRequired(Network networkfromDB, Network networkFromMessage) {
        networkfromDB = networkDAO.findByName(networkfromDB.getName()); //???


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
