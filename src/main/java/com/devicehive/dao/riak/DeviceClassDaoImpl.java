package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.DeleteValue;
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
    public boolean isExist(long id) {
        return false;
    }

    @Override
    public DeviceClass getReference(String name) {
        return find(name);
    }

    @Override
    public void remove(DeviceClass reference) {
        try {
            Location location = new Location(DEVICE_CLASS_NS, reference.getId());
            DeleteValue delete = new DeleteValue.Builder(location).build();
            client.execute(delete);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DeviceClass find(String id) {
        try {
            Location location = new Location(DEVICE_CLASS_NS, id);
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
            StoreValue storeOp = new StoreValue.Builder(deviceClass)
                    .withLocation(location).build();
            client.execute(storeOp);
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
