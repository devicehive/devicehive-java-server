package com.devicehive.service;

import com.devicehive.dao.AccessKeyDAO;
import com.devicehive.dao.AccessKeyPermissionDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.model.updates.AccessKeyUpdate;
import com.devicehive.service.helpers.AccessKeyProcessor;
import com.devicehive.utils.LogExecutionTime;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

@Stateless
@LogExecutionTime
@EJB(beanInterface = AccessKeyService.class, name = "AccessKeyService")
public class AccessKeyService {

    @EJB
    private AccessKeyDAO accessKeyDAO;

    @EJB
    private AccessKeyPermissionDAO permissionDAO;

    @EJB
    private UserService userService;

    public AccessKey create (@NotNull User user, @NotNull AccessKey accessKey){
        if (accessKey.getLabel() == null){
            throw new HiveException("Label is required!", Response.Status.BAD_REQUEST.getStatusCode());
        }
        if (accessKey.getId() != null || accessKey.getPermissions() == null){
            throw new HiveException(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE, Response.Status.BAD_REQUEST.getStatusCode());
        }
        validateActions(accessKey);
        AccessKeyProcessor keyProcessor = new AccessKeyProcessor();
        String key = keyProcessor.generateKey();
        accessKey.setKey(key);
        accessKey.setUser(user);
        accessKeyDAO.insert(accessKey);
        for (AccessKeyPermission permission : accessKey.getPermissions()){
            permission.setAccessKey(accessKey);
            permissionDAO.insert(permission);
        }
        return accessKey;
    }

    public boolean update(@NotNull Long userId, @NotNull Long keyId, AccessKeyUpdate toUpdate){
        AccessKey existing = accessKeyDAO.get(userId, keyId);
        if (existing == null){
            return false;
        }
        if (toUpdate == null){
            return true;
        }
        if (toUpdate.getLabel() != null){
            existing.setLabel(toUpdate.getLabel().getValue());
        }
        if (toUpdate.getExpirationDate()!= null){
            existing.setExpirationDate(toUpdate.getExpirationDate().getValue());
        }
        if (toUpdate.getPermissions() != null){
            Set<AccessKeyPermission> permissionsToReplace = toUpdate.getPermissions().getValue();
            if (permissionsToReplace == null){
                throw new HiveException(ErrorResponse.INVALID_REQUEST_PARAMETERS_MESSAGE, Response.Status.BAD_REQUEST.getStatusCode());
            }
            AccessKey toValidate = toUpdate.convertTo();
            validateActions(toValidate);
            permissionDAO.deleteByAccessKey(existing);
            for (AccessKeyPermission current : permissionsToReplace){
                current.setAccessKey(existing);
                permissionDAO.insert(current);
            }
        }
        return true;
    }

    public AccessKey authenticate(@NotNull String key){
        return accessKeyDAO.get(key);
    }

    private void validateActions(AccessKey accessKey){
        Set<String> actions = new HashSet<>();
        for (AccessKeyPermission permission : accessKey.getPermissions()){
            if (permission.getActions() == null){
                throw new HiveException("Actions is required!", Response.Status.BAD_REQUEST.getStatusCode());
            }
            actions.addAll(permission.getActionsAsSet());
        }
        if (!AvailableActions.validate(actions)){
            throw new HiveException("Unknown action!", Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    public boolean hasAcccessToNetwork(AccessKey accessKey, Network targetNetwork){
        Set<AccessKeyPermission> permissions = accessKey.getPermissions();
        Set<Long> allowedNetworks = new HashSet<>();
        User user = accessKey.getUser();
        for (AccessKeyPermission currentPermission : permissions){
            allowedNetworks.addAll(currentPermission.getNetworkIdsAsSet());
        }
        if (allowedNetworks.contains(null)){
            return user.getNetworks().contains(targetNetwork);
        }
        return user.getNetworks().contains(targetNetwork) && allowedNetworks.contains(targetNetwork.getId());
    }

    public boolean hasAccessToDevice(AccessKey accessKey, Device device){
        Set<AccessKeyPermission> permissions = accessKey.getPermissions();
        Set<String> allowedDevices = new HashSet<>();
        User user = accessKey.getUser();
        for (AccessKeyPermission currentPermission : permissions){
            allowedDevices.addAll(currentPermission.getDeviceGuidsAsSet());
        }
        if (allowedDevices.contains(null)){
            return userService.hasAccessToDevice(user, device);
        }
        return userService.hasAccessToDevice(user, device) && allowedDevices.contains(device.getGuid());
    }
}
