package com.devicehive.dao.graph.model;


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
        vo.setStatus(v.property(Properties.STATUS).isPresent() ? (String) v.property(Properties.STATUS).value() : null);
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

        if (vo.getStatus() != null) {
            gT.property(Properties.STATUS, vo.getStatus());
        } else {
            throw new HivePersistenceLayerException("Device status cannot be null");
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
