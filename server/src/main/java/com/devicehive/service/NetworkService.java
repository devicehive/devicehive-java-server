package com.devicehive.service;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.CheckPermissionsHelper;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.NetworkDAO;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.updates.NetworkUpdate;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import java.util.*;

import static javax.ws.rs.core.Response.Status.*;

@Stateless
public class NetworkService {
    private NetworkDAO networkDAO;
    private UserService userService;
    private AccessKeyService accessKeyService;
    private DeviceService deviceService;

    @EJB
    public void setDeviceService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

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

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Network getWithDevicesAndDeviceClasses(@NotNull Long networkId, @NotNull HivePrincipal principal) {
        if (principal.getUser() != null) {
            List<Network> found = networkDAO.getNetworkList(principal.getUser(), null, Arrays.asList(networkId));
            if (found.isEmpty()) {
                return null;
            }
            List<Device> devices = deviceService.getList(networkId, principal.getUser(), null);
            Network result = found.get(0);
            result.setDevices(new HashSet<Device>(devices));
            return result;
        } else {
            AccessKey key = principal.getKey();
            User user = userService.findUserWithNetworks(key.getUser().getId());
            List<Network> found = networkDAO.getNetworkList(user,
                    key.getPermissions(),
                    Arrays.asList(networkId));
            Network result = found.isEmpty() ? null : found.get(0);
            if (result == null) {
                return result;
            }
            //to get proper devices 1) get access key with all permissions 2) get devices for required network
            key = accessKeyService.find(key.getId(), principal.getKey().getUser().getId());
            List<AllowedKeyAction.Action> actions = new ArrayList<>();
            actions.add(AllowedKeyAction.Action.GET_DEVICE);
            if (!CheckPermissionsHelper.checkAllPermissions(key, actions)) {
                result.setDevices(null);
                return result;
            }
            Set<Device> devices =
                    new HashSet<>(deviceService.getList(result.getId(), key.getUser(), key.getPermissions()));
            result.setDevices(devices);
            return result;
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
    public List<Network> list(String name,
                              String namePattern,
                              String sortField,
                              boolean sortOrder,
                              Integer take,
                              Integer skip,
                              User user,
                              Collection<AccessKeyBasedFilterForDevices> extraFilters) {
        return networkDAO.list(name, namePattern, sortField, sortOrder, take, skip, user, extraFilters);
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
