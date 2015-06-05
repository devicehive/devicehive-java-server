package com.devicehive.resource.impl;


import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.resource.ConfigurationResource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URI;

@Service
@Path("/configuration")
public class ConfigurationResourceImpl implements ConfigurationResource {

    @Autowired
    private ConfigurationService configurationService;

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
    public Response auto(String referrer) {
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
