package com.devicehive.controller;

import com.devicehive.auth.AllowedKeyAction;
import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyRequest;
import com.devicehive.model.oauth.IdentityProviderEnum;
import com.devicehive.service.AccessKeyService;
import com.devicehive.util.LogExecutionTime;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.devicehive.auth.AllowedKeyAction.Action.MANAGE_ACCESS_KEY;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * Created by tmatvienko on 12/2/14.
 */
@Path("/auth/accesskey")
@LogExecutionTime
public class AuthAccessKeyController {

    @EJB
    private AccessKeyService accessKeyService;
    @Inject
    private HiveSecurityContext hiveSecurityContext;

    @POST
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(AccessKeyRequest request) {
        final IdentityProviderEnum identityProviderEnum = IdentityProviderEnum.forName(request.getProviderName());
        AccessKey accessKey = accessKeyService.createAccessKey(request, identityProviderEnum);
        return ResponseFactory.response(OK, accessKey, JsonPolicyDef.Policy.ACCESS_KEY_SUBMITTED);
    }

    @DELETE
    @RolesAllowed({HiveRoles.KEY})
    @AllowedKeyAction(action = MANAGE_ACCESS_KEY)
    public Response logout() {
        AccessKey accessKey = hiveSecurityContext.getHivePrincipal().getKey();
        accessKeyService.delete(null, accessKey.getId());
        return ResponseFactory.response(NO_CONTENT);
    }
}
