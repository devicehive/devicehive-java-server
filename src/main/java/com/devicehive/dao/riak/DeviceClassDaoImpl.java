package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.model.DeviceClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Profile({"riak"})
@Repository
public class DeviceClassDaoImpl implements DeviceClassDao {

    private static final Namespace DEVICE_CLASS_NS = new Namespace("deviceClass");

    @Autowired
    private RiakClient client;

    @Override
    public DeviceClass findByNameAndVersion(String name, String version) {
        return null;
    }

    @Override
    public boolean isExist(long id) {
        return false;
    }

    @Override
    public DeviceClass getReference(long id) {
        return null;
    }

    @Override
    public void remove(DeviceClass reference) {

    }

    @Override
    public DeviceClass find(long id) {
        try {
            Location location = new Location(DEVICE_CLASS_NS, String.valueOf(id));
            FetchValue fetchOp = new FetchValue.Builder(location)
                    .build();
            return client.execute(fetchOp).getValue(DeviceClass.class);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void refresh(DeviceClass stored, LockModeType lockModeType) {

    }

    @Override
    public void persist(DeviceClass deviceClass) {
        try {
            Location location = new Location(DEVICE_CLASS_NS, String.valueOf(deviceClass.getId()));
            StoreValue sroreOp = new StoreValue.Builder(deviceClass).withLocation(location).build();
            client.execute(sroreOp);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DeviceClass merge(DeviceClass deviceClass) {
        return null;
    }

    @Override
    public List<DeviceClass> getDeviceClassList(String name, String namePattern, String version, String sortField, Boolean sortOrderAsc, Integer take, Integer skip) {
        return null;
    }
}
