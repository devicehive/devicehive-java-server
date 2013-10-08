package com.devicehive.service;

import com.devicehive.dao.NetworkDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.updates.NetworkUpdate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.*;

@Stateless
public class NetworkService {
    private NetworkDAO networkDAO;
    private UserService userService;
    private AccessKeyService accessKeyService;

    @EJB
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @EJB
    public void setNetworkDAO(NetworkDAO networkDAO) {
        this.networkDAO = networkDAO;
    }

    @EJB
    public void setAccessKeyService(AccessKeyService accessKeyService) {
        this.accessKeyService = accessKeyService;
    }

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
            throw new HiveException(ErrorResponse.NETWORK_NOT_FOUND_MESSAGE, NOT_FOUND.getStatusCode());
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
                    throw new HiveException("Wrong network key!", FORBIDDEN.getStatusCode());
                }
            }
        } else {
            if (update.getId() != null) {
                throw new HiveException("Invalid request", BAD_REQUEST.getStatusCode());
            }
            stored = networkDAO.createNetwork(update);
        }
        assert (stored != null);
        return stored;
    }

    public Network createOrUpdateNetworkByUser(NullableWrapper<Network> network, User user) {
        Network stored;

        //case network is not defined
        if (network == null || network.getValue() == null) {
            return null;
        }

        Network update = network.getValue();

        if (update.getId() != null) {
            stored = networkDAO.getWithDevicesAndDeviceClasses(update.getId());
        } else {
            stored = networkDAO.findByName(update.getName());
        }

        if (stored != null) {
            if (stored.getKey() != null) {
                if (!stored.getKey().equals(update.getKey())) {
                    throw new HiveException("Wrong network key!", FORBIDDEN.getStatusCode());
                }
            }
            if (!userService.hasAccessToNetwork(user, stored)) {
                throw new HiveException("No access to network!", FORBIDDEN.getStatusCode());
            }
        } else if (user.isAdmin()) {
            if (update.getId() != null) {
                throw new HiveException("Invalid request", BAD_REQUEST.getStatusCode());
            }
            stored = networkDAO.createNetwork(update);

        } else {
            throw new HiveException("No permissions to create network!", FORBIDDEN.getStatusCode());
        }
        return stored;
    }

    public Network verifyNetworkByKey(NullableWrapper<Network> network, AccessKey key) {
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
                    throw new HiveException("Wrong network key!", FORBIDDEN.getStatusCode());
                }
                if (!accessKeyService.hasAccessToNetwork(key, stored)) {
                    throw new HiveException("No permissions to access network!", FORBIDDEN.getStatusCode());
                }
            }
        }
        return stored;
    }
}
