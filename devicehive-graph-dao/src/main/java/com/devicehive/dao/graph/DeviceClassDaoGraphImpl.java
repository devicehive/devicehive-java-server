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

import com.devicehive.dao.DeviceClassDao;
import com.devicehive.dao.graph.model.DeviceClassVertex;
import com.devicehive.vo.DeviceClassVO;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DeviceClassDaoGraphImpl extends GraphGenericDao implements DeviceClassDao {

    @Override
    public void remove(long id) {
        GraphTraversal<Vertex, Vertex> gT = g.V().has(DeviceClassVertex.LABEL, DeviceClassVertex.Properties.ID, id);
        gT.drop().iterate();
    }

    @Override
    public DeviceClassVO find(long id) {
        GraphTraversal<Vertex, Vertex> gT = g.V().has(DeviceClassVertex.LABEL, DeviceClassVertex.Properties.ID, id);

        if (gT.hasNext()) {
            return DeviceClassVertex.toVO(gT.next());
        } else {
            return null;
        }
    }

    @Override
    public DeviceClassVO persist(DeviceClassVO deviceClass) {
        //TODO - major performance bottleneck, look into more efficient ID generation mechanisms
        if (deviceClass.getId() == null) {
            long id = g.V().hasLabel(DeviceClassVertex.LABEL).count().next();
            deviceClass.setId(id);
        }

        GraphTraversal<Vertex, Vertex> gT = DeviceClassVertex.toVertex(deviceClass, g);
        return DeviceClassVertex.toVO(gT.next());
    }

    @Override
    public DeviceClassVO merge(DeviceClassVO deviceClass) {
        GraphTraversal<Vertex, Vertex> gT = g.V()
                .hasLabel(DeviceClassVertex.LABEL)
                .has(DeviceClassVertex.Properties.ID, deviceClass.getId());

        gT.property(DeviceClassVertex.Properties.NAME, deviceClass.getName());
        gT.property(DeviceClassVertex.Properties.IS_PERMANENT, deviceClass.getIsPermanent());
        gT.property(DeviceClassVertex.Properties.DATA, deviceClass.getData());
        gT.property(DeviceClassVertex.Properties.ENTITY_VERSION, deviceClass.getEntityVersion());
        gT.next();
        return deviceClass;
    }

    @Override
    public List<DeviceClassVO> list(String name, String namePattern, String sortField, Boolean sortOrderAsc, Integer take, Integer skip) {
        GraphTraversal<Vertex, Vertex> gT = g.V()
                .hasLabel(DeviceClassVertex.LABEL);

        if (name != null) {
            gT.has(DeviceClassVertex.Properties.NAME, name);
        }

        if (namePattern != null) {
            //fixme - implement
        }

        if (sortField != null) {
            if (sortOrderAsc != null) {
                gT.order().by(sortField).by(sortOrderAsc ? Order.incr : Order.decr);
            } else {
                gT.order().by(sortField);
            }
        }

        if (take != null) {
            gT.limit(take);
        }

        if (skip != null) {
            gT.range(skip, -1L);
        }

        List<DeviceClassVO> result = new ArrayList<>();

        while (gT.hasNext()) {
            result.add(DeviceClassVertex.toVO(gT.next()));
        }

        return result;
    }

    @Override
    public DeviceClassVO findByName(@NotNull String name) {
        GraphTraversal<Vertex, Vertex> gT = g.V().has(DeviceClassVertex.LABEL, DeviceClassVertex.Properties.NAME, name);

        if (gT.hasNext()) {
            return DeviceClassVertex.toVO(gT.next());
        } else {
            return null;
        }
    }
}
