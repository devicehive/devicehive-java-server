package com.devicehive.dao;

import com.devicehive.model.Device;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.Network;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


@Repository
public class GenericDatabaseAccessDAO {

    @Autowired
    private DataSource rdbmsDataSource;

    public Device findDevice(final String deviceGuid) {
        String sql = "";
        sql = "select d.id, d.guid, d.name, d.status, d.device_class_id, d.key, d.data, d.entity_version, d.blocked, n.id, n.name, n.description, n.key, n.entity_version from device d left outer join network n on d.network_id = n.id where guid = ?";
        try (Connection conn = rdbmsDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, deviceGuid);
            try (ResultSet rs = ps.executeQuery();) {
                while (rs.next()) {
                    Device device = new Device();

                    Network network = new Network();
                    network.setId(rs.getLong(10));
                    if (!rs.wasNull()) {
                        network.setName(rs.getString(11));
                        network.setDescription(rs.getString(12));
                        network.setKey(rs.getString(13));
                        network.setEntityVersion(rs.getLong(14));
                    } else {
                        network = null;
                    }

                    DeviceClass deviceClass = new DeviceClass();
                    device.setId(rs.getLong(5));

                    device.setNetwork(network);
                    device.setDeviceClass(deviceClass);

                    device.setId(rs.getLong(1));
                    device.setGuid(rs.getString(2));
                    device.setName(rs.getString(3));
                    device.setStatus(rs.getString(4));
                    device.setKey(rs.getString(6));
                    device.setData(new JsonStringWrapper(rs.getString(7)));
                    device.setEntityVersion(rs.getLong(8));
                    device.setBlocked(rs.getBoolean(9));

                    return device;
                }
            }
        } catch (SQLException e) {
            return null;
        }

        return null;
    }

}
