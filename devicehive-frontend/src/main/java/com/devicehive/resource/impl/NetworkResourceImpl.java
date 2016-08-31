package com.devicehive.resource.impl;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.updates.NetworkUpdate;
import com.devicehive.resource.NetworkResource;
import com.devicehive.resource.converters.SortOrderQueryParamParser;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.NetworkService;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.List;

import static com.devicehive.configuration.Constants.ID;
import static com.devicehive.configuration.Constants.NAME;
import static javax.ws.rs.core.Response.Status.*;

@Service
public class NetworkResourceImpl implements NetworkResource {
    private static final Logger logger = LoggerFactory.getLogger(NetworkResourceImpl.class);

    @Autowired
    private NetworkService networkService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Response list(String name, String namePattern, String sortField, String sortOrderSt, Integer take, Integer skip) {

        logger.debug("Network list requested");

        boolean sortOrder = SortOrderQueryParamParser.parse(sortOrderSt);

        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !NAME.equalsIgnoreCase(sortField)) {
            logger.error("Unable to proceed network list request. Invalid sortField");
            return ResponseFactory.response(Response.Status.BAD_REQUEST,
                    new ErrorResponse(Messages.INVALID_REQUEST_PARAMETERS));
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<NetworkVO> result = networkService
            .list(name, namePattern, sortField, sortOrder, take, skip, principal);

        logger.debug("Network list request proceed successfully.");
        return ResponseFactory.response(Response.Status.OK, result, JsonPolicyDef.Policy.NETWORKS_LISTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response get(long id) {
        logger.debug("Network get requested.");
        NetworkWithUsersAndDevicesVO existing = networkService.getWithDevicesAndDeviceClasses(id, (HiveAuthentication) SecurityContextHolder.getContext().getAuthentication());
        if (existing == null) {
            logger.error("Network with id =  {} does not exists", id);
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.NETWORK_NOT_FOUND, id)));
        }
        return ResponseFactory.response(OK, existing, JsonPolicyDef.Policy.NETWORK_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insert(NetworkVO network) {
        logger.debug("Network insert requested");
        NetworkVO result = networkService.create(network);
        logger.debug("New network has been created");
        return ResponseFactory.response(CREATED, result, JsonPolicyDef.Policy.NETWORK_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response update(NetworkUpdate networkToUpdate, long id) {
        logger.debug("Network update requested. Id : {}", id);
        networkService.update(id, networkToUpdate);
        logger.debug("Network has been updated successfully. Id : {}", id);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response delete(long id) {
        logger.debug("Network delete requested");
        networkService.delete(id);
        logger.debug("Network with id = {} does not exists any more.", id);
        return ResponseFactory.response(NO_CONTENT);
    }
}