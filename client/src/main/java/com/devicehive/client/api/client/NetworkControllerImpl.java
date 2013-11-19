package com.devicehive.client.api.client;


import com.devicehive.client.context.HiveContext;
import com.devicehive.client.model.Network;
import com.google.common.reflect.TypeToken;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.devicehive.client.json.strategies.JsonPolicyDef.Policy.*;

public class NetworkControllerImpl implements NetworkController {

    private final HiveContext hiveContext;

    public NetworkControllerImpl(HiveContext hiveContext) {
        this.hiveContext = hiveContext;
    }

    @Override
    public List<Network> listNetworks(String name, String namePattern, String sortField, String sortOrder, Integer take,
                                      Integer skip) {
        String path = "/network";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("name", name);
        queryParams.put("namePattern", namePattern);
        queryParams.put("sortField", sortField);
        queryParams.put("sortOrder", sortOrder);
        queryParams.put("take", take);
        queryParams.put("skip", skip);
        return hiveContext.getHiveRestClient()
                .execute(path, HttpMethod.GET, null, null, new TypeToken<List<Network>>() {
                }.getType(), NETWORKS_LISTED);
    }

    @Override
    public Network getNetwork(long id) {
        String path = "/network/" + id;
        return hiveContext.getHiveRestClient().execute(path, HttpMethod.GET, null, Network.class, NETWORK_PUBLISHED);
    }

    @Override
    public long insertNetwork(Network network) {
        String path = "/network";
        Network returned = hiveContext.getHiveRestClient().execute(path, HttpMethod.POST, null, null,
                network, Network.class, NETWORK_UPDATE, NETWORK_SUBMITTED);
        return returned.getId();
    }

    @Override
    public void updateNetwork(long id, Network network) {
        String path = "/network/" + id;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.PUT, null, network, NETWORK_UPDATE);
    }

    @Override
    public void deleteNetwork(long id) {
        String path = "/network/" + id;
        hiveContext.getHiveRestClient().execute(path, HttpMethod.DELETE);
    }
}
