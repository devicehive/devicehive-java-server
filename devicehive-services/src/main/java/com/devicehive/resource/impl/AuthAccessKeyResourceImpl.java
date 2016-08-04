package com.devicehive.resource.impl;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.AccessKey;
import com.devicehive.vo.AccessKeyRequestVO;
import com.devicehive.model.oauth.IdentityProviderEnum;
import com.devicehive.resource.AuthAccessKeyResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.AccessKeyService;
import com.devicehive.vo.AccessKeyVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * Created by tmatvienko on 12/2/14.
 */
@Service
public class AuthAccessKeyResourceImpl implements AuthAccessKeyResource {

    @Autowired
    private AccessKeyService accessKeyService;

    @Override
    public Response login(AccessKeyRequestVO request) {
        final IdentityProviderEnum identityProviderEnum = IdentityProviderEnum.forName(request.getProviderName());
        AccessKeyVO accessKey = accessKeyService.createAccessKey(request, identityProviderEnum);
        return ResponseFactory.response(OK, accessKey, JsonPolicyDef.Policy.ACCESS_KEY_SUBMITTED);
    }

    @Override
    public Response logout() {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AccessKeyVO accessKey = principal.getKey();
        accessKeyService.delete(null, accessKey.getId());
        return ResponseFactory.response(NO_CONTENT);
    }
}
