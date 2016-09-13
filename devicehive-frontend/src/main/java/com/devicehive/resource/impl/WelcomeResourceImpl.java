package com.devicehive.resource.impl;

import com.devicehive.configuration.Constants;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.resource.WelcomeResource;
import com.devicehive.resource.util.ResponseFactory;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

@Service
public class WelcomeResourceImpl implements WelcomeResource {

    @Override
    public Response getWelcomeInfo() {
        return ResponseFactory.response(Response.Status.OK, Constants.WELCOME_MESSAGE,
                JsonPolicyDef.Policy.REST_SERVER_INFO);
    }
}

