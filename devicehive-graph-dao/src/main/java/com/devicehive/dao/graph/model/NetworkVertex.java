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
import com.devicehive.vo.NetworkVO;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class NetworkVertex {

    public static final String LABEL = "Network";

    public static NetworkVO toVO(Vertex v) {
        NetworkVO vo = new NetworkVO();
        vo.setId((Long) v.property(Properties.ID).value());
        vo.setKey(v.property(Properties.KEY).isPresent() ? (String) v.property(Properties.KEY).value() : null);
        vo.setName(v.property(Properties.NAME).isPresent() ? (String) v.property(Properties.NAME).value() : null);
        vo.setDescription(v.property(Properties.DESCRIPTION).isPresent() ? (String) v.property(Properties.DESCRIPTION).value() : null);
        vo.setEntityVersion(v.property(Properties.ENTITY_VERSION).isPresent() ? (long) v.property(Properties.ENTITY_VERSION).value() : null);
        return vo;
    }

    public static GraphTraversal<Vertex, Vertex> toVertex(NetworkVO vo, GraphTraversalSource g) {
        GraphTraversal<Vertex, Vertex> gT = g.addV(NetworkVertex.LABEL);

        gT.property(Properties.ID, vo.getId());

        gT.property(Properties.KEY, vo.getKey());

        if (vo.getName() != null) {
            gT.property(Properties.NAME, vo.getName());
        } else {
            throw new HivePersistenceLayerException("Network name cannot be null");
        }

        gT.property(Properties.DESCRIPTION, vo.getDescription());
        gT.property(Properties.ENTITY_VERSION, vo.getEntityVersion());

        return gT;
    }

    public class Properties {
        public static final String ID = "dh_id";
        public static final String KEY = "key";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String ENTITY_VERSION = "entity_version";
    }
}
