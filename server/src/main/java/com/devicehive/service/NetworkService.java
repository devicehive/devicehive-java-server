package com.devicehive.service;

import com.devicehive.dao.NetworkDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.Network;
import com.devicehive.model.NullableWrapper;
import com.devicehive.model.User;
import com.devicehive.model.updates.NetworkUpdate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

@Stateless
public class NetworkService {


    @EJB
    private NetworkDAO networkDAO;

    public Network getById(long id) {
        return networkDAO.getById(id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Network getWithDevicesAndDeviceClasses(long id, User user) {
        if (user.isAdmin()) {
            return networkDAO.getWithDevicesAndDeviceClasses(id);
        } else {
            return networkDAO.getWithDevicesAndDeviceClasses(id, user.getId());
        }

    }

    public boolean delete(long id) {
        return networkDAO.delete(id);
    }

    public Network create(Network newNetwork) {
        if (newNetwork.getId() != null) {
            throw new HiveException("Invalid request. Id cannot be specified.", BAD_REQUEST.getStatusCode());
        }
        Network existing = networkDAO.findByName(newNetwork.getName());
        if (existing != null) {
            throw new HiveException("Network cannot be created. Network with such name already exists",
                    FORBIDDEN.getStatusCode());
        }
        return networkDAO.createNetwork(newNetwork);
    }

    public Network update(@NotNull Long networkId, NetworkUpdate networkUpdate) {
        Network existing = getById(networkId);
        if (existing == null) {
            throw new HiveException(ErrorResponse.NETWORK_NOT_FOUND_MESSAGE,
                    Response.Status.NOT_FOUND.getStatusCode());
        }
        if (networkUpdate.getKey() != null) {
            existing.setKey(networkUpdate.getKey().getValue());
        }
        if (networkUpdate.getName() != null) {
            existing.setName(networkUpdate.getName().getValue());
        }
        if (networkUpdate.getDescription() != null) {
            existing.setDescription(networkUpdate.getDescription().getValue());
        }
        return existing;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Network> list(String name, String namePattern,
                              String sortField, boolean sortOrder,
                              Integer take, Integer skip, Long userId, Set<Long> allowedIds) {
        return networkDAO.list(name, namePattern, sortField, sortOrder, take, skip, userId, allowedIds);
    }

    public Network createOrVeriryNetwork(NullableWrapper<Network> network) {
        Network stored;
        //case network is not defined
        if (network == null || network.getValue() == null) {
            return null;
        }

        Network update = network.getValue();

        if (update.getId() != null) {
            stored = networkDAO.getById(update.getId());
        } else {
            stored = networkDAO.findByName(update.getName());
        }

        if (stored != null) {
            if (stored.getKey() != null) {
                if (!stored.getKey().equals(update.getKey())) {
                    throw new HiveException("Wrong network key!", 403);
                }
            }
        } else {
            if (update.getId() != null) {
                throw new HiveException("Invalid request");
            }
            stored = networkDAO.createNetwork(update);
        }
        assert (stored != null);
        return stored;
    }
}
