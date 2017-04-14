package com.devicehive.dao.graph;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
 * %%
 * Copyright (C) 2016 - 2017 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.graph.model.DeviceVertex;
import com.devicehive.dao.graph.model.NetworkVertex;
import com.devicehive.dao.graph.model.Relationship;
import com.devicehive.dao.graph.model.UserVertex;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.NetworkWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class NetworkDaoGraphImpl extends GraphGenericDao implements NetworkDao {

    private static final Logger logger = LoggerFactory.getLogger(NetworkDaoGraphImpl.class);

    @Override
    public List<NetworkVO> findByName(String name) {
        logger.info("Getting network by name");
        GraphTraversal<Vertex, Vertex> gT = g.V().has(NetworkVertex.LABEL, NetworkVertex.Properties.NAME, name);

        List<NetworkVO> networks = new ArrayList<>();

        while (gT.hasNext()) {
            NetworkVO networkVO = NetworkVertex.toVO(gT.next());
            networks.add(networkVO);
        }

        return networks;
    }

    @Override
    public void persist(NetworkVO newNetwork) {
        logger.info("Adding network");

        //TODO - major performance bottleneck, look into more efficient ID generation mechanisms
        if (newNetwork.getId() == null) {
            long id = g.V().hasLabel(NetworkVertex.LABEL).count().next();
            newNetwork.setId(id);
        }

        GraphTraversal<Vertex, Vertex> gT = NetworkVertex.toVertex(newNetwork, g);
        gT.next();
        logger.info(g.V().count().next().toString());
    }

    @Override
    public List<NetworkWithUsersAndDevicesVO> getNetworksByIdsAndUsers(Long idForFiltering, Set<Long> networkdIds, Set<Long> permittedNetworks) {
        Set<Long> intersection = networkdIds;
        if (permittedNetworks != null) {
            intersection = networkdIds.stream()
                    .filter(permittedNetworks::contains)
                    .collect(Collectors.toSet());
        }
        Stream<NetworkWithUsersAndDevicesVO> networkStream = intersection.stream()
                .map(this::findWithUsers)
                .filter(Optional::isPresent)
                .map(Optional::get);
        if (idForFiltering != null) {
            networkStream = networkStream.filter(n -> n.getUsers().stream().anyMatch(u -> u.getId().equals(idForFiltering)));
        }
        return networkStream.collect(Collectors.toList());
    }

    @Override
    public int deleteById(long id) {
        logger.info("Deleting network");
        GraphTraversal<Vertex, Vertex> gT = g.V().has(NetworkVertex.LABEL, NetworkVertex.Properties.ID, id);
        int count = gT.asAdmin()
                .clone()
                .toList()
                .size();

        gT.drop().iterate();
        logger.info(g.V().count().next().toString());
        return count;
    }

    @Override
    public NetworkVO find(@NotNull Long networkId) {
        logger.info("Getting network by id");
        GraphTraversal<Vertex, Vertex> gT = g.V().has(NetworkVertex.LABEL, NetworkVertex.Properties.ID, networkId);

        if (gT.hasNext()) {
            return NetworkVertex.toVO(gT.next());
        } else {
            return null;
        }
    }

    @Override
    public NetworkVO merge(NetworkVO existing) {
        logger.info("Updating network");
        GraphTraversal<Vertex, Vertex> gT = g.V()
                .hasLabel(NetworkVertex.LABEL)
                .has(NetworkVertex.Properties.ID, existing.getId());

        gT.property(NetworkVertex.Properties.NAME, existing.getName());
        gT.property(NetworkVertex.Properties.KEY, existing.getKey());
        gT.property(NetworkVertex.Properties.DESCRIPTION, existing.getDescription());
        gT.property(NetworkVertex.Properties.ENTITY_VERSION, existing.getEntityVersion());
        gT.next();
        return existing;
    }

    @Override
    public void assignToNetwork(NetworkVO network, UserVO user) {
        g.V().hasLabel(NetworkVertex.LABEL)
                .has(NetworkVertex.Properties.ID, network.getId())
                .as("n")
                .V()
                .hasLabel(UserVertex.LABEL)
                .has(UserVertex.Properties.ID, user.getId())
                .addE(Relationship.IS_MEMBER_OF).to("n")
                .iterate();
    }

    @Override
    public List<NetworkVO> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take, Integer skip, Optional<HivePrincipal> principal) {
        return null;
    }

    @Override
    public Optional<NetworkVO> findFirstByName(String name) {
        logger.info("Getting network by name");
        GraphTraversal<Vertex, Vertex> gT = g.V().has(NetworkVertex.LABEL, NetworkVertex.Properties.NAME, name);

        if (gT.hasNext()) {
            return Optional.of(NetworkVertex.toVO(gT.next()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<NetworkWithUsersAndDevicesVO> findWithUsers(@NotNull long networkId) {
        logger.info("Getting network with users and devices by id");
        GraphTraversal<Vertex, Vertex> gT = g.V().has(NetworkVertex.LABEL, NetworkVertex.Properties.ID, networkId);

        if (gT.hasNext()) {
            NetworkVO networkVO = NetworkVertex.toVO(gT.next());
            NetworkWithUsersAndDevicesVO result = new NetworkWithUsersAndDevicesVO(networkVO);

            GraphTraversal<Vertex, Vertex> gTU = g.V().has(NetworkVertex.LABEL, NetworkVertex.Properties.ID, networkId).in(Relationship.IS_MEMBER_OF);
            Set<UserVO> users = new HashSet<>();
            while (gTU.hasNext()) {
                UserVO userVO = UserVertex.toVO(gTU.next());
                users.add(userVO);
            }
            result.setUsers(users);

            GraphTraversal<Vertex, Vertex> gTD = g.V().has(NetworkVertex.LABEL, NetworkVertex.Properties.ID, networkId).in(Relationship.BELONGS_TO);
            Set<DeviceVO> devices = new HashSet<>();
            while (gTD.hasNext()) {
                DeviceVO deviceVO = DeviceVertex.toVO(gTD.next());
                devices.add(deviceVO);
            }
            result.setDevices(devices);

            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }
}
