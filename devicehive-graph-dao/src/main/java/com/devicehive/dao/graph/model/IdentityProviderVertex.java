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
import com.devicehive.vo.IdentityProviderVO;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class IdentityProviderVertex {

    public static final String LABEL = "IdentityProvider";

    public static IdentityProviderVO toVO(Vertex v) {
        IdentityProviderVO vo = new IdentityProviderVO();
        vo.setName(v.property(Properties.NAME).isPresent() ? (String) v.property(Properties.NAME).value() : null);
        vo.setApiEndpoint(v.property(Properties.API_ENDPOINT).isPresent() ? (String) v.property(Properties.API_ENDPOINT).value() : null);
        vo.setVerificationEndpoint(v.property(Properties.VERIFICATION_ENDPOINT).isPresent() ? (String) v.property(Properties.VERIFICATION_ENDPOINT).value() : null);
        vo.setTokenEndpoint(v.property(Properties.TOKEN_ENDPOINT).isPresent() ? (String) v.property(Properties.TOKEN_ENDPOINT).value() : null);
        return vo;
    }

    public static GraphTraversal<Vertex, Vertex> toVertex(IdentityProviderVO vo, GraphTraversalSource g) {
        GraphTraversal<Vertex, Vertex> gT = g.addV(IdentityProviderVertex.LABEL);

        if (vo.getName() != null) {
            gT.property(Properties.NAME, vo.getName());
        } else {
            throw new HivePersistenceLayerException("Network name cannot be null");
        }

        gT.property(Properties.API_ENDPOINT, vo.getApiEndpoint());
        gT.property(Properties.VERIFICATION_ENDPOINT, vo.getVerificationEndpoint());
        gT.property(Properties.TOKEN_ENDPOINT, vo.getTokenEndpoint());

        return gT;
    }

    public class Properties {
        public static final String NAME = "name";
        public static final String API_ENDPOINT = "api_endpoint";
        public static final String VERIFICATION_ENDPOINT = "verification_endpoint";
        public static final String TOKEN_ENDPOINT = "token_endpoint";
    }
}
