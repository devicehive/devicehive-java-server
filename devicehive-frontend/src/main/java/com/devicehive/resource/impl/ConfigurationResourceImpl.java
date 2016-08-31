package com.devicehive.resource.impl;


import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.resource.ConfigurationResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

@Service
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

}
