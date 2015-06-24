package com.devicehive.resource.impl;


import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.ConfigurationResource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Service
@Path("/configuration")
public class ConfigurationResourceImpl implements ConfigurationResource {

    @Autowired
    private ConfigurationService configurationService;

    @Value("${server.context-path}")
    private String contextPath;

    @Override
    public Response get(String name) {
        return Response.ok().entity(configurationService.get(name)).build();
    }

    @Override
    public Response setProperty(String name, String value) {
        configurationService.save(name, value);
        return Response.ok().build();
    }

    @Override
    public Response setPropertyGet(String name, String value) {
        configurationService.save(name, value);
        return Response.ok().build();
    }

    @Override
    public Response deleteProperty(String name) {
        configurationService.delete(name);
        return Response.noContent().build();
    }

    @Override
    public Response auto(String referer, UriInfo uriInfo) {
        try {
            URI ref = URI.create(referer);

            if (!("http".equals(ref.getScheme()) || "https".equals(ref.getScheme()))) {
                throw new WebApplicationException(
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(new ErrorResponse("Wrong referer"))
                                .build());
            }

            String restUri = uriInfo.getBaseUri().toString();
            String wesocketUri = "ws://" + uriInfo.getBaseUri().getHost() + ":" + uriInfo.getBaseUri().getPort() + contextPath + "/websocket";
            configurationService.save(Constants.REST_SERVER_URL, restUri);
            configurationService.save(Constants.WEBSOCKET_SERVER_URL, wesocketUri);
            return Response.seeOther(ref).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

}
