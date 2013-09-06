package com.devicehive.service;

import com.devicehive.dao.AccessKeyDAO;
import com.devicehive.dao.AccessKeyPermissionDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
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
}
