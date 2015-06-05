package com.devicehive.resource;

import com.devicehive.model.AccessKeyRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public interface AuthAccessKeyResource {

    @POST
    @PreAuthorize("permitAll")
    @Consumes(MediaType.APPLICATION_JSON)
    Response login(AccessKeyRequest request);

    @DELETE
    @PreAuthorize("hasRole('KEY') and hasPermission(null, 'MANAGE_ACCESS_KEY')")
    Response logout();

}
