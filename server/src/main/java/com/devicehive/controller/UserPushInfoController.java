package com.devicehive.controller;

import static com.devicehive.auth.AllowedKeyAction.Action.MANAGE_PUSH_INFO;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.configuration.Messages;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.User;
import com.devicehive.model.UserPushInfo;
import com.devicehive.model.updates.UserPushInfoUpdate;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.service.UserPushService;
import com.devicehive.service.UserService;
import com.devicehive.util.LogExecutionTime;

@Path("/user/pushinfo")
@LogExecutionTime
public class UserPushInfoController {

	@Inject
    private HiveSecurityContext hiveSecurityContext;
	
	@EJB
    private UserPushService userPushService;
	
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({HiveRoles.CLIENT, HiveRoles.ADMIN, HiveRoles.KEY})
    @AllowedKeyAction(action = MANAGE_PUSH_INFO)
    @JsonPolicyDef(JsonPolicyDef.Policy.USERS_LISTED)
    public Response insertUser(UserPushInfoUpdate userPushInfoUpdate) {
    	
    	HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        Long userId = principal.getUser() != null ? principal.getUser().getId() : principal.getKey().getUser().getId();
    	
        UserPushInfo userPushInfo = userPushService.saveUserPushInfo(userId, userPushInfoUpdate);
        if (userPushInfo == null) {
        	return ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                                      Messages.INVALID_REQUEST_PARAMETERS));
        }
        
        return ResponseFactory.response(CREATED, null, null);
        
    }
}
