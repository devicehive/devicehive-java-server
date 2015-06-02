package com.devicehive.resource;


import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.WwwAuthenticateRequired;
import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * Provide API information
 */
@Service
@Path("/configuration")
public class ConfigurationResource {

    @Autowired
    private ConfigurationService configurationService;


    @GET
    @PreAuthorize("hasRole('ADMIN')")
    @Path("/{name}")
    @WwwAuthenticateRequired
    public Response get(@PathParam("name") String name) {
        return Response.ok().entity(configurationService.get(name)).build();
    }

    @PUT
    @PreAuthorize("hasRole('ADMIN')")
    @RolesAllowed(HiveRoles.ADMIN)
    @Path("/{name}")
    @WwwAuthenticateRequired
    public Response setProperty(@PathParam("name") String name, String value) {
        configurationService.save(name, value);
        return Response.ok().build();
    }

    @GET
    @PreAuthorize("hasRole('ADMIN')")
    @Path("/{name}/set")
    @WwwAuthenticateRequired
    public Response setPropertyGet(@PathParam("name") String name, @QueryParam("value") String value) {
        configurationService.save(name, value);
        return Response.ok().build();
    }

    @DELETE
    @PreAuthorize("hasRole('ADMIN')")
    @Path("/{name}")
    @WwwAuthenticateRequired
    public Response deleteProperty(@PathParam("name") String name) {
        configurationService.delete(name);
        return Response.noContent().build();
    }


    @POST
    //fixme: make index.html show auth prompt on configuration action
//    @PreAuthorize("hasRole('ADMIN')")
    @Path("/auto")
    @WwwAuthenticateRequired
    public Response auto(@HeaderParam(HttpHeaders.REFERER) String referrer) {
        try {
            URI ref = checkURI(referrer);
            String refString = ref.toString();
            String restUri = StringUtils.removeEnd(refString, "/") + "/rest";
            String
                wesocketUri =
                StringUtils.removeEnd("ws" + StringUtils.removeStart(refString, "http"), "/") + "/websocket";
            configurationService.save(Constants.REST_SERVER_URL, restUri);
            configurationService.save(Constants.WEBSOCKET_SERVER_URL, wesocketUri);
            return Response.seeOther(ref).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }


    private URI checkURI(String referrer) {
        URI uri = URI.create(referrer);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw new IllegalArgumentException();
        }
        return uri;
    }


}
