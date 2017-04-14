package com.devicehive.dao.graph.model;

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


import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.vo.DeviceVO;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class DeviceVertex {

    public static final String LABEL = "Device";

    public static DeviceVO toVO(Vertex v) {
        DeviceVO vo = new DeviceVO();
        vo.setId((Long) v.property(Properties.ID).value());
        vo.setGuid(v.property(Properties.GUID).isPresent() ? (String) v.property(Properties.GUID).value() : null);
        vo.setName(v.property(Properties.NAME).isPresent() ? (String) v.property(Properties.NAME).value() : null);
        vo.setData(new JsonStringWrapper(v.property(Properties.DATA).isPresent() ? (String) v.property(Properties.DATA).value() : null));
        vo.setBlocked(v.property(Properties.BLOCKED).isPresent() ? (Boolean) v.property(Properties.BLOCKED).value() : null);
        return vo;
    }

    public static GraphTraversal<Vertex, Vertex> toVertex(DeviceVO vo, GraphTraversalSource g) {
        GraphTraversal<Vertex, Vertex> gT = g.addV(DeviceVertex.LABEL);

        gT.property(Properties.ID, vo.getId());

        if (vo.getGuid() != null) {
            gT.property(Properties.GUID, vo.getGuid());
        } else {
            throw new HivePersistenceLayerException("Device guid cannot be null");
        }

        if (vo.getName() != null) {
            gT.property(Properties.NAME, vo.getName());
        } else {
            throw new HivePersistenceLayerException("Device name cannot be null");
        }

        gT.property(Properties.DATA, vo.getData());
        gT.property(Properties.BLOCKED, vo.getBlocked());

        return gT;
    }

    public class Properties {
        public static final String ID = "dh_id";
        public static final String GUID = "guid";
        public static final String NAME = "name";
        public static final String STATUS = "status";
        public static final String DATA = "data";
        public static final String BLOCKED = "blocked";
    }
}
