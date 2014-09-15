package com.devicehive.client;


import com.devicehive.client.model.Network;
import com.devicehive.client.model.exceptions.HiveException;

import java.util.List;

/**
 * Client side controller for network: <i>/network</i> See <a href="http://www.devicehive.com/restful/#Reference/Network">DeviceHive
 * RESTful API: Network</a> for details. Transport declared in the hive context will be used.
 */
public interface NetworkController {

    /**
     * Queries list of networks using following criteria: See: <a href="http://www.devicehive.com/restful#Reference/Network/list">DeviceHive
     * RESTful API: Network: list</a> for more details.
     *
     * @param name        exact network's name, ignored, when  namePattern is not null
     * @param namePattern name pattern
     * @param sortField   sort Field, can be either "id", "key", "name" or "description"
     * @param sortOrder   Result list sort order. Available values are ASC and DESC.
     * @param take        limit, default 1000
     * @param skip        offset, default 0
     * @return list of <a href="http://www.devicehive.com/restful#Reference/Network">networks</a>
     */
    List<Network> listNetworks(String name, String namePattern, String sortField, String sortOrder, Integer take,
                               Integer skip) throws HiveException;

    /**
     * Gets information about network. See: <a href="http://www.devicehive.com/restful#Reference/Network/get">DeviceHive
     * RESTful API: Network: get</a> for more details.
     *
     * @param id network identifier
     * @return If successful, this method returns a <a href="http://www.devicehive.com/restful#Reference/Network">network</a>
     *         resource in the response body.
     */
    Network getNetwork(long id) throws HiveException;

    /**
     * Creates new network. See: <a href="http://www.devicehive.com/restful#Reference/Network/insert">DeviceHive RESTful
     * API: Network: insert</a> for more details.
     *
     * @param network network to be inserted
     * @return network identifier
     */
    long insertNetwork(Network network) throws HiveException;

    /**
     * Updates existing network. See: <a href="http://www.devicehive.com/restful#Reference/Network/update">DeviceHive
     * RESTful API: Network: update</a> for more details.
     *
     * @param network network to be updated
     */
    void updateNetwork(Network network) throws HiveException;

    /**
     * Deletes network by its identifier. See: <a href="http://www.devicehive.com/restful#Reference/Network/delete">DeviceHive
     * RESTful API: Network: delete</a> for more details.
     *
     * @param id network identifier
     */
    void deleteNetwork(long id) throws HiveException;
}
