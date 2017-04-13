package com.devicehive.dao.graph.model;


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
