package com.devicehive.service;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.CheckPermissionsHelper;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.AccessKeyDAO;
import com.devicehive.dao.NetworkDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.updates.NetworkUpdate;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.google.common.collect.Sets;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Stateless
public class NetworkService {
    @EJB
    private NetworkDAO networkDAO;
    @EJB
    private UserService userService;
    @EJB
    private AccessKeyService accessKeyService;
    @EJB
    private AccessKeyDAO accessKeyDAO;
    @EJB
    private DeviceService deviceService;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Network getWithDevicesAndDeviceClasses(@NotNull Long networkId, @NotNull HivePrincipal principal) {
        if (principal.getUser() != null) {
            List<Network> found = networkDAO.getNetworkList(principal.getUser(), null, Arrays.asList(networkId));
            if (found.isEmpty()) {
                return null;
            }
            List<Device> devices = deviceService.getList(networkId, principal);
            Network result = found.get(0);
            result.setDevices(new HashSet<>(devices));
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
            AccessKey currentKey = accessKeyDAO.getWithoutUser(user.getId(), key.getId());
            Set<AccessKeyPermission> filtered = CheckPermissionsHelper.filterPermissions(key.getPermissions(), AllowedKeyAction.Action.GET_DEVICE, ThreadLocalVariablesKeeper.getClientIP(), ThreadLocalVariablesKeeper.getHostName());
            if (filtered.isEmpty()) {
                result.setDevices(null);
                return result;
            }
            Set<Device> devices =
                    new HashSet<>(deviceService.getList(result.getId(), principal));
            result.setDevices(devices);
            return result;
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean delete(long id) {
        return networkDAO.delete(id);
    }

    public Network create(Network newNetwork) {
        if (newNetwork.getId() != null) {
            throw new HiveException(Messages.ID_NOT_ALLOWED, BAD_REQUEST.getStatusCode());
        }
        Network existing = networkDAO.findByName(newNetwork.getName());
        if (existing != null) {
            throw new HiveException(Messages.DUPLICATE_NETWORK, FORBIDDEN.getStatusCode());
        }
        return networkDAO.createNetwork(newNetwork);
    }

    public Network update(@NotNull Long networkId, NetworkUpdate networkUpdate) {
        Network existing = getById(networkId);
        if (existing == null) {
            throw new HiveException(String.format(Messages.NETWORK_NOT_FOUND, networkId), NOT_FOUND.getStatusCode());
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

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<Network> list(String name,
                              String namePattern,
                              String sortField,
                              boolean sortOrder,
                              Integer take,
                              Integer skip,
                              HivePrincipal principal) {
        return networkDAO.list(name, namePattern, sortField, sortOrder, take, skip, principal);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
                    throw new HiveException(Messages.INVALID_NETWORK_KEY, FORBIDDEN.getStatusCode());
                }
            }
        } else {
            if (update.getId() != null) {
                throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, BAD_REQUEST.getStatusCode());
            }
            stored = networkDAO.createNetwork(update);
        }
        assert (stored != null);
        return stored;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
                    throw new HiveException(Messages.INVALID_NETWORK_KEY, FORBIDDEN.getStatusCode());
                }
            }
            if (!userService.hasAccessToNetwork(user, stored)) {
                throw new HiveException(Messages.NO_ACCESS_TO_NETWORK, FORBIDDEN.getStatusCode());
            }
        } else if (user.isAdmin()) {
            if (update.getId() != null) {
                throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, BAD_REQUEST.getStatusCode());
            }
            stored = networkDAO.createNetwork(update);

        } else {
            throw new HiveException(Messages.NETWORK_CREATION_NOT_ALLOWED, FORBIDDEN.getStatusCode());
        }
        return stored;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
                    throw new HiveException(Messages.INVALID_NETWORK_KEY, FORBIDDEN.getStatusCode());
                }
                if (!accessKeyService.hasAccessToNetwork(key, stored)) {
                    throw new HiveException(Messages.NO_ACCESS_TO_NETWORK, FORBIDDEN.getStatusCode());
                }
            }
        }
        return stored;
    }

    private Network getById(long id) {
        return networkDAO.getById(id);
    }
}
