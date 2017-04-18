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
import com.devicehive.dao.DeviceDao;
import com.devicehive.dao.graph.model.DeviceClassVertex;
import com.devicehive.dao.graph.model.DeviceVertex;
import com.devicehive.dao.graph.model.NetworkVertex;
import com.devicehive.dao.graph.model.Relationship;
import com.devicehive.vo.DeviceVO;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DeviceDaoGraphImpl extends GraphGenericDao implements DeviceDao {

    @Override
    public DeviceVO findByUUID(String uuid) {
        GraphTraversal<Vertex, Vertex> gT = g.V().has(DeviceVertex.LABEL, DeviceVertex.Properties.GUID, uuid);

        if (gT.hasNext()) {
            return DeviceVertex.toVO(gT.next());
        } else {
            return null;
        }
    }

    @Override
    public void persist(DeviceVO device) {
        //TODO - major performance bottleneck, look into more efficient ID generation mechanisms
        if (device.getId() == null) {
            long id = g.V().hasLabel(DeviceVertex.LABEL).count().next();
            device.setId(id);
        }

        GraphTraversal<Vertex, Vertex> gT = DeviceVertex.toVertex(device, g);
        gT.next();

        g.V().hasLabel(NetworkVertex.LABEL)
                .has(NetworkVertex.Properties.ID, device.getNetwork().getId())
                .as("n")
                .V()
                .hasLabel(DeviceVertex.LABEL)
                .has(DeviceVertex.Properties.ID, device.getId())
                .addE(Relationship.BELONGS_TO)
                .to("n")
                .iterate();
    }

    @Override
    public DeviceVO merge(DeviceVO device) {
        GraphTraversal<Vertex, Vertex> gT = g.V()
                .hasLabel(DeviceVertex.LABEL)
                .has(DeviceVertex.Properties.ID, device.getId());

        gT.property(DeviceVertex.Properties.GUID, device.getGuid());
        gT.property(DeviceVertex.Properties.NAME, device.getName());
        gT.property(DeviceVertex.Properties.DATA, device.getData());
        gT.property(DeviceVertex.Properties.BLOCKED, device.getBlocked());
        gT.next();
        return device;
    }

    @Override
    public int deleteByUUID(String guid) {
        GraphTraversal<Vertex, Vertex> gT = g.V().has(DeviceVertex.LABEL, DeviceVertex.Properties.GUID, guid);
        int count = gT.asAdmin()
                .clone()
                .toList()
                .size();

        gT.drop().iterate();
        return count;
    }

    @Override
    public List<DeviceVO> getDeviceList(List<String> guids, HivePrincipal principal) {
        List<DeviceVO> result = new ArrayList<>();
        for (String guid : guids) {
            if (principal.getDeviceGuids().contains(guid)) {
                result.add(findByUUID(guid));
            }
        }

        return result;
    }

    @Override
    public long getAllowedDeviceCount(HivePrincipal principal, List<String> guids) {
        long count = 0;

        for (String guid : guids) {
            if (principal.getDeviceGuids().contains(guid)) {
                count++;
            }
        }

        return count;
    }

    @Override
    public List<DeviceVO> list(String name, String namePattern, Long networkId, String networkName,
                               Long deviceClassId, String deviceClassName,
                               String sortField, Boolean sortOrderAsc, Integer take, Integer skip, HivePrincipal principal) {
        GraphTraversal<Vertex, Vertex> gT = g.V()
                .hasLabel(NetworkVertex.LABEL);

        if (name != null) {
            gT.has(DeviceVertex.Properties.NAME, name);
        }

        if (namePattern != null) {
            //fixme - implement
        }

        if (networkId != null) {
            gT.outE(Relationship.BELONGS_TO)
                    .where(__.otherV().hasLabel(NetworkVertex.LABEL).has(NetworkVertex.Properties.ID, networkId));
        }

        if (networkName != null) {
            gT.outE(Relationship.BELONGS_TO)
                    .where(__.otherV().hasLabel(NetworkVertex.LABEL).has(NetworkVertex.Properties.NAME, networkName));
        }

        if (deviceClassId != null) {
            gT.outE(Relationship.IS_A)
                    .where(__.otherV().hasLabel(DeviceClassVertex.LABEL).has(DeviceClassVertex.Properties.ID, deviceClassId));
        }

        if (deviceClassName != null) {
            gT.outE(Relationship.IS_A)
                    .where(__.otherV().hasLabel(DeviceClassVertex.LABEL).has(DeviceClassVertex.Properties.NAME, deviceClassName));
        }

        if (sortField != null) {
            gT.order().by(sortField).by(sortOrderAsc ? Order.incr : Order.decr);
        }

        if (take != null) {
            gT.limit(take);
        }

        if (skip != null) {
            gT.range(skip, -1L);
        }

        List<DeviceVO> devices = new ArrayList<>();

        while (gT.hasNext()) {
            devices.add(DeviceVertex.toVO(gT.next()));
        }

        if (principal == null || principal.areAllNetworksAvailable()) {
            return devices;
        } else {
            return devices.stream()
                    .filter(vo -> principal.getNetworkIds().contains(vo.getId()))
                    .collect(Collectors.toList());
        }
    }
}
