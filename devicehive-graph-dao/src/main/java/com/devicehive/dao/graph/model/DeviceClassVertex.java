package com.devicehive.dao.graph.model;

import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.vo.DeviceClassVO;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class DeviceClassVertex {

    public static final String LABEL = "DeviceClass";

    public static DeviceClassVO toVO(Vertex v) {
        DeviceClassVO vo = new DeviceClassVO();
        vo.setId((Long) v.property(Properties.ID).value());
        vo.setName(v.property(Properties.NAME).isPresent() ? (String) v.property(Properties.NAME).value() : null);
        vo.setIsPermanent(v.property(Properties.IS_PERMANENT).isPresent() ? (Boolean) v.property(Properties.IS_PERMANENT).value() : null);
        vo.setOfflineTimeout(v.property(Properties.OFFLINE_TIMEOUT).isPresent() ? (Integer) v.property(Properties.OFFLINE_TIMEOUT).value() : null);
        vo.setData(new JsonStringWrapper(v.property(Properties.DATA).isPresent() ? (String) v.property(Properties.DATA).value() : null));
        vo.setEntityVersion(v.property(Properties.ENTITY_VERSION).isPresent() ? (long) v.property(Properties.ENTITY_VERSION).value() : null);
        return vo;
    }

    public static GraphTraversal<Vertex, Vertex> toVertex(DeviceClassVO vo, GraphTraversalSource g) {
        GraphTraversal<Vertex, Vertex> gT = g.addV(DeviceClassVertex.LABEL);
        gT.property(Properties.ID, vo.getId());

        if (vo.getName() != null) {
            gT.property(Properties.NAME, vo.getName());
        } else {
            throw new HivePersistenceLayerException("Network name cannot be null");
        }

        gT.property(Properties.IS_PERMANENT, vo.getIsPermanent());
        gT.property(Properties.OFFLINE_TIMEOUT, vo.getOfflineTimeout());
        gT.property(Properties.DATA, vo.getData());
        gT.property(Properties.ENTITY_VERSION, vo.getEntityVersion());

        return gT;
    }

    public class Properties {
        public static final String ID = "dh_id";
        public static final String NAME = "name";
        public static final String IS_PERMANENT = "is_permanent";
        public static final String OFFLINE_TIMEOUT = "offline_timeout";
        public static final String DATA = "data";
        public static final String ENTITY_VERSION = "entity_version";
    }
}
