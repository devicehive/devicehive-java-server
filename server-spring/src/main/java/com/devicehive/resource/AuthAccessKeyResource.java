package com.devicehive.resource;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyRequest;
import com.devicehive.model.oauth.IdentityProviderEnum;
import com.devicehive.service.AccessKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * Created by tmatvienko on 12/2/14.
 */
@Singleton
@Path("/auth/accesskey")
public class AuthAccessKeyResource {

    @Autowired
    private AccessKeyService accessKeyService;

    @POST
    @PreAuthorize("permitAll")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(AccessKeyRequest request) {
        final IdentityProviderEnum identityProviderEnum = IdentityProviderEnum.forName(request.getProviderName());
        AccessKey accessKey = accessKeyService.createAccessKey(request, identityProviderEnum);
        return ResponseFactory.response(OK, accessKey, JsonPolicyDef.Policy.ACCESS_KEY_SUBMITTED);
    }

    @DELETE
    @PreAuthorize("hasRole('KEY') and hasPermission(null, 'MANAGE_ACCESS_KEY')")
    public Response logout() {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AccessKey accessKey = principal.getKey();
        accessKeyService.delete(null, accessKey.getId());
        return ResponseFactory.response(NO_CONTENT);
    }
}
