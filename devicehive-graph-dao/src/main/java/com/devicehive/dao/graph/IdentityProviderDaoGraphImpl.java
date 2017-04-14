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

import com.devicehive.dao.IdentityProviderDao;
import com.devicehive.dao.graph.model.IdentityProviderVertex;
import com.devicehive.vo.IdentityProviderVO;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;

@Repository
public class IdentityProviderDaoGraphImpl extends GraphGenericDao implements IdentityProviderDao {

    @Override
    public IdentityProviderVO getByName(@NotNull String name) {
        GraphTraversal<Vertex, Vertex> gT = g.V().has(IdentityProviderVertex.LABEL, IdentityProviderVertex.Properties.NAME, name);
        if (gT.hasNext()) {
            return IdentityProviderVertex.toVO(gT.next());
        } else {
            return null;
        }
    }

    @Override
    public boolean deleteById(@NotNull String name) {
        GraphTraversal<Vertex, Vertex> gT = g.V().has(IdentityProviderVertex.LABEL, IdentityProviderVertex.Properties.NAME, name);

        if (gT.hasNext()) {
            gT.drop().iterate();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public IdentityProviderVO merge(IdentityProviderVO existing) {
        GraphTraversal<Vertex, Vertex> gT = g.V()
                .hasLabel(IdentityProviderVertex.LABEL)
                .has(IdentityProviderVertex.Properties.NAME, existing.getName());
        gT.property(IdentityProviderVertex.Properties.NAME, existing.getName());
        gT.property(IdentityProviderVertex.Properties.API_ENDPOINT, existing.getApiEndpoint());
        gT.property(IdentityProviderVertex.Properties.VERIFICATION_ENDPOINT, existing.getVerificationEndpoint());
        gT.property(IdentityProviderVertex.Properties.TOKEN_ENDPOINT, existing.getTokenEndpoint());
        gT.next();
        return existing;
    }

    @Override
    public void persist(IdentityProviderVO identityProvider) {
        IdentityProviderVertex.toVertex(identityProvider, g).next();
    }
}
