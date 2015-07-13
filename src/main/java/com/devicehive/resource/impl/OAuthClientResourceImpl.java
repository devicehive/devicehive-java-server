package com.devicehive.resource.impl;


import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.OAuthClient;
import com.devicehive.model.updates.OAuthClientUpdate;
import com.devicehive.resource.OAuthClientResource;
import com.devicehive.resource.converters.SortOrderQueryParamParser;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.OAuthClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.*;

@Service
public class OAuthClientResourceImpl implements OAuthClientResource {
    private static final Logger logger = LoggerFactory.getLogger(OAuthClientResourceImpl.class);

    @Autowired
    private OAuthClientService clientService;

    @Override
    public Response list(String name, String namePattern, String domain, String oauthId, String sortField, String sortOrderSt, Integer take, Integer skip) {
        boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);

        if (sortField != null && !sortField.equalsIgnoreCase(ID) && !sortField.equalsIgnoreCase(NAME) &&
                !sortField.equalsIgnoreCase(DOMAIN) && !sortField.equalsIgnoreCase(OAUTH_ID)) {
            return ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }

        List<OAuthClient> result =
                clientService.get(name, namePattern, domain, oauthId, sortField, sortOrder, take, skip);
        logger.debug("OAuthClient list procced. Params: name {}, namePattern {}, domain {}, oauthId {}, " +
                        "sortField {}, sortOrder {}, take {}, skip {}. Result list contains {} elems", name, namePattern,
                domain, oauthId, sortField, sortOrder, take, skip, result.size());

        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal != null && principal.getUser() != null && principal.getUser().isAdmin()) {
            return ResponseFactory.response(OK, result, OAUTH_CLIENT_LISTED_ADMIN);
        }
        return ResponseFactory.response(OK, result, OAUTH_CLIENT_LISTED);
    }

    @Override
    public Response get(long clientId) {
        logger.debug("OAuthClient get requested. Client id: {}", clientId);
        OAuthClient existing = clientService.get(clientId);
        if (existing == null) {
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(),
                            "OAuthClient with id " + clientId + " not found"));
        }
        logger.debug("OAuthClient proceed successfully. Client id: {}", clientId);
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal != null && principal.getUser() != null && principal.getUser().isAdmin()) {
            return ResponseFactory.response(OK, existing, OAUTH_CLIENT_LISTED_ADMIN);
        }
        return ResponseFactory.response(OK, existing, OAUTH_CLIENT_LISTED);
    }

    @Override
    public Response insert(OAuthClient clientToInsert) {
        logger.debug("OAuthClient insert requested. Client to insert: {}", clientToInsert);
        if (clientToInsert == null) {
            return ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
        }
        OAuthClient created = clientService.insert(clientToInsert);
        logger.debug("OAuthClient insert procceed successfully. Client to insert: {}. New id: {}", clientToInsert,
                clientToInsert.getId());
        return ResponseFactory.response(CREATED, created, OAUTH_CLIENT_PUBLISHED);
    }

    @Override
    public Response update(Long clientId, OAuthClientUpdate clientToUpdate) {
        logger.debug("OAuthClient update requested. Client id: {}", clientId);
        clientService.update(clientToUpdate, clientId);
        logger.debug("OAuthClient update proceed successfully. Client id: {}", clientId);
        return ResponseFactory.response(NO_CONTENT);
    }

    @Override
    public Response delete(Long clientId) {
        logger.debug("OAuthClient delete requested");
        clientService.delete(clientId);
        logger.debug("OAuthClient with id = {} is deleted", clientId);
        return ResponseFactory.response(NO_CONTENT);
    }
}