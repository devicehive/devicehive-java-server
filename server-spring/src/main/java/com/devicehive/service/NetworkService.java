package com.devicehive.service;

import com.devicehive.auth.AccessKeyAction;
import com.devicehive.auth.CheckPermissionsHelper;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.NetworkDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.updates.NetworkUpdate;
import com.devicehive.util.HiveValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;

import static javax.ws.rs.core.Response.Status.*;

@Component
public class NetworkService {

    public static final String ALLOW_NETWORK_AUTO_CREATE = "allowNetworkAutoCreate";

    @Autowired
    private NetworkDAO networkDAO;
    @Autowired
    private UserService userService;
    @Autowired
    private AccessKeyService accessKeyService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private HiveValidator hiveValidator;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Network getWithDevicesAndDeviceClasses(@NotNull Long networkId,
                                                  @NotNull HiveAuthentication hiveAuthentication) {
        HiveAuthentication.HiveAuthDetails details = (HiveAuthentication.HiveAuthDetails) hiveAuthentication.getDetails();
        HivePrincipal principal = (HivePrincipal) hiveAuthentication.getPrincipal();
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
            Set<AccessKeyPermission> filtered = CheckPermissionsHelper
                .filterPermissions(key.getPermissions(), AccessKeyAction.GET_DEVICE,
                                   details.getClientInetAddress(), details.getOrigin());
            if (filtered.isEmpty()) {
                result.setDevices(Collections.emptySet());
                return result;
            }
            Set<Device> devices = new HashSet<>(deviceService.getList(result.getId(), principal));
            result.setDevices(devices);
            return result;
        }
    }

    public boolean delete(long id) {
        return networkDAO.delete(id);
    }

    @Transactional
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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
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
        hiveValidator.validate(existing);
        return networkDAO.updateNetwork(existing);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Network> list(String name,
                              String namePattern,
                              String sortField,
                              boolean sortOrder,
                              Integer take,
                              Integer skip,
                              HivePrincipal principal) {
        return networkDAO.list(name, namePattern, sortField, sortOrder, take, skip, principal);
    }

    @Transactional
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
            if (configurationService.getBoolean(ALLOW_NETWORK_AUTO_CREATE, false)) {
                stored = networkDAO.createNetwork(update);
            }
        }
        assert (stored != null);
        return stored;
    }

    @Transactional
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
        } else {
            if (update.getId() != null) {
                throw new HiveException(Messages.NETWORK_NOT_FOUND, BAD_REQUEST.getStatusCode());
            }
            if (user.isAdmin() || configurationService.getBoolean(ALLOW_NETWORK_AUTO_CREATE, false)) {
                stored = networkDAO.createNetwork(update);
            } else {
                throw new HiveException(Messages.NETWORK_CREATION_NOT_ALLOWED, FORBIDDEN.getStatusCode());
            }
        }
        return stored;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Network createOrVeriryNetworkByKey(NullableWrapper<Network> network, AccessKey key) {
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
        } else {
            if (configurationService.getBoolean(ALLOW_NETWORK_AUTO_CREATE, false)) {
                stored = networkDAO.createNetwork(update);
            } else {
                throw new HiveException(Messages.NETWORK_CREATION_NOT_ALLOWED, FORBIDDEN.getStatusCode());
            }
        }
        return stored;
    }

    private Network getById(long id) {
        return networkDAO.getById(id);
    }
}