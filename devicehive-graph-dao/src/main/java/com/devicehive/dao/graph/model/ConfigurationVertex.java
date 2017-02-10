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
import com.devicehive.vo.ConfigurationVO;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class ConfigurationVertex {

    public static final String LABEL = "Configuration";

    public static ConfigurationVO toVO(Vertex v) {
        ConfigurationVO configurationVO = new ConfigurationVO();
        configurationVO.setName((String) v.property(Properties.NAME).value());
        configurationVO.setValue((String) v.property(Properties.VALUE).value());
        configurationVO.setEntityVersion((long) v.property(Properties.ENTITY_VERSION).value());
        return configurationVO;
    }

    public static GraphTraversal<Vertex, Vertex> toVertex(ConfigurationVO vo, GraphTraversalSource g) {
        GraphTraversal<Vertex, Vertex> gT = g.addV(ConfigurationVertex.LABEL);

        if (vo.getName() != null) {
            gT.property(ConfigurationVertex.Properties.NAME, vo.getName());
        } else {
            throw new HivePersistenceLayerException("Configuration name cannot be null");
        }

        if (vo.getValue() != null) {
            gT.property(ConfigurationVertex.Properties.VALUE, vo.getValue());
        } else {
            throw new HivePersistenceLayerException("Configuration value cannot be null");
        }

        gT.property(ConfigurationVertex.Properties.ENTITY_VERSION, vo.getEntityVersion());

        return gT;
    }

    public class Properties {
        public static final String NAME = "name";
        public static final String VALUE = "value";
        public static final String ENTITY_VERSION = "entity_version";
    }
}
