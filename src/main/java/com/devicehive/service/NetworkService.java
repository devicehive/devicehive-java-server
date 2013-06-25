package com.devicehive.service;

import com.devicehive.dao.NetworkDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.Network;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: jkulagina
 * Date: 24.06.13
 * Time: 20:23
 * To change this template use File | Settings | File Templates.
 */
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
            createNetwork(networkFromMessage);
            network = networkFromMessage;
        } else {
            network = updateNetworkIfRequired(network, networkFromMessage);
        }
        return network;
    }

    private void createNetwork(Network network) {
        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
        Validator validator = vf.getValidator();
        Set<String> validationErrorsSet = Network.validate(network, validator);
        if (validationErrorsSet.isEmpty()) {
            networkDAO.addNetwork(network);
        } else {
            String exceptionMessage = "Validation faild: ";
            for (String violation : validationErrorsSet) {
                exceptionMessage += violation + "\n";
            }
            throw new HiveException(exceptionMessage);
        }
    }

    private Network updateNetworkIfRequired(Network networkfromDB, Network networkFromMessage) {
        if (networkfromDB.getKey() != null) {
            if (!networkfromDB.getKey().equals(networkFromMessage.getKey())) {
                throw new HiveException("Wrong network key!");
            }
        }
        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
        Validator validator = vf.getValidator();
        Set<String> validationErrorsSet = Network.validate(networkFromMessage, validator);
        if (validationErrorsSet.isEmpty()) {
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
        } else {
            String exceptionMessage = "Validation faild: ";
            for (String violation : validationErrorsSet) {
                exceptionMessage += violation + "\n";
            }
            throw new HiveException(exceptionMessage);
        }
        return networkfromDB;
    }

}
