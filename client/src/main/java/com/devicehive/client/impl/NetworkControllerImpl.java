package com.devicehive.client.impl;


import com.devicehive.client.NetworkController;
import com.devicehive.client.impl.context.HiveContext;
import com.devicehive.client.model.Network;
import com.devicehive.client.model.exceptions.HiveClientException;
import com.google.common.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

class NetworkControllerImpl implements NetworkController {

    private static final Logger logger = LoggerFactory.getLogger(NetworkControllerImpl.class);
    private final HiveContext hiveContext;

    public NetworkControllerImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @Override
    public List<Network> listNetworks(String name, String namePattern, String sortField, String sortOrder, Integer take,
                                      Integer skip) {
        logger.debug("Network: list requested with parameters: name {}, name pattern {}, sort field {}, " +
                "sort order {}, take {}, skip {}", name, namePattern, sortField, sortOrder, take, skip);
        String path = "/network";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("name", name);
        queryParams.put("namePattern", namePattern);
        queryParams.put("sortField", sortField);
        queryParams.put("sortOrder", sortOrder);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        List<Network> result = hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.GET, null, null, new TypeToken<List<Network>>() {
                }.getType(), NETWORKS_LISTED);
        logger.debug("Network: list request proceed with parameters: name {}, name pattern {}, sort field {}, " +
                "sort order {}, take {}, skip {}", name, namePattern, sortField, sortOrder, take, skip);
        return result;
    }

    @Override
    public Network getNetwork(long id) {
        logger.debug("Network: get requested for network with id {}", id);
        String path = "/network/" + id;
        Network result = hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, Network.class,
                NETWORK_PUBLISHED);
        logger.debug("Network: get requested for network with id {}. Network name {}", id, result.getName());
        return result;
    }

    @Override
    public long insertNetwork(Network network) {
        if (network == null) {
            throw new HiveClientException("Network cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("Network: insert requested for network with name {}", network.getName());
        String path = "/network";
        Network returned = hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null,
                network, Network.class, NETWORK_UPDATE, NETWORK_SUBMITTED);
        logger.debug("Network: insert request proceed for network with name {}. Result id {}", network.getName(),
                returned.getId());
        return returned.getId();
    }

    @Override
    public void updateNetwork(long id, Network network) {
        if (network == null) {
            throw new HiveClientException("Network cannot be null!", BAD_REQUEST.getStatusCode());
        }
        logger.debug("Network: update requested for network with name {} and id {}", network.getName(), id);
        String path = "/network/" + id;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, network, NETWORK_UPDATE);
        logger.debug("Network: update request proceed for network with name {} and id {}", network.getName(), id);
    }

    @Override
    public void deleteNetwork(long id) {
        logger.debug("Network: delete requested for network with id {}", id);
        String path = "/network/" + id;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
        logger.debug("Network: delete request proceed for network with id {}", id);
    }
}
