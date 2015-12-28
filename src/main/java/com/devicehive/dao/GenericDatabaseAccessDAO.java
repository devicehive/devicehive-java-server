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
        final String sql = "select d.id, d.guid, d.name, d.status, d.device_class_id, d.data, d.entity_version, d.blocked, n.id, n.name, n.description, n.key, n.entity_version from device d left outer join network n on d.network_id = n.id where guid = ?";
        try (Connection conn = rdbmsDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, deviceGuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Device device = new Device();

                    Network network = new Network();
                    network.setId(rs.getLong(9));
                    if (!rs.wasNull()) {
                        network.setName(rs.getString(10));
                        network.setDescription(rs.getString(11));
                        network.setKey(rs.getString(12));
                        network.setEntityVersion(rs.getLong(13));
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
                    device.setData(new JsonStringWrapper(rs.getString(6)));
                    device.setEntityVersion(rs.getLong(7));
                    device.setBlocked(rs.getBoolean(8));

                    return device;
                }
            }
        } catch (SQLException e) {
            return null;
        }

        return null;
    }

}
