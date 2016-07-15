package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.datatypes.CounterUpdate;
import com.basho.riak.client.api.commands.datatypes.FetchCounter;
import com.basho.riak.client.api.commands.datatypes.UpdateCounter;
import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.dao.CacheConfig;
import com.devicehive.dao.DeviceDao;
import com.devicehive.dao.DeviceEquipmentDao;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.Optional.of;

@Profile({"riak"})
@Repository
public class DeviceEquipmentDaoImpl implements DeviceEquipmentDao {

    private static final Namespace COUNTER_NS = new Namespace("counters", "device_equipment_counters");
    private static final Namespace DEVICE_EQUIPMENT_NS = new Namespace("device_equipment");

    @Autowired
    private RiakClient client;

    @Override
    public List<DeviceEquipment> getByDevice(Device device) {
        BinIndexQuery biq = new BinIndexQuery.Builder(DEVICE_EQUIPMENT_NS, "device", device.getGuid()).build();
        try {
            List<DeviceEquipment> deviceEquipmentList = new ArrayList<>();
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return null;
            }
            for (BinIndexQuery.Response.Entry e : entries) {
                Location location = e.getRiakObjectLocation();
                FetchValue fetchOp = new FetchValue.Builder(location)
                        .build();
                DeviceEquipment deviceEquipment = client.execute(fetchOp).getValue(DeviceEquipment.class);
                deviceEquipmentList.add(deviceEquipment);
            }

            return deviceEquipmentList;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public DeviceEquipment getByDeviceAndCode(@NotNull String code, @NotNull Device device) {
        BinIndexQuery biq = new BinIndexQuery.Builder(DEVICE_EQUIPMENT_NS, "device", device.getGuid()).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return null;
            }
            for (BinIndexQuery.Response.Entry e : entries) {
                Location location = e.getRiakObjectLocation();
                FetchValue fetchOp = new FetchValue.Builder(location)
                        .build();
                DeviceEquipment deviceEquipment = client.execute(fetchOp).getValue(DeviceEquipment.class);
                if (deviceEquipment.getCode().equals(code)) {
                    return deviceEquipment;
                }
            }

            return null;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void persist(DeviceEquipment deviceEquipment) {
        merge(deviceEquipment);
    }

    @Override
    public DeviceEquipment merge(DeviceEquipment deviceEquipment) {
        Location deviceEquipmentCounters = new Location(COUNTER_NS, "device_equipment_counter");
        try {
            if (deviceEquipment.getId() == null) {
                CounterUpdate cu = new CounterUpdate(1);
                UpdateCounter update = new UpdateCounter.Builder(deviceEquipmentCounters, cu).build();
                client.execute(update);
                FetchCounter fetch = new FetchCounter.Builder(deviceEquipmentCounters).build();
                Long id = client.execute(fetch).getDatatype().view();
                deviceEquipment.setId(id);
            }
            Location location = new Location(DEVICE_EQUIPMENT_NS, String.valueOf(deviceEquipment.getId()));
            StoreValue storeOp = new StoreValue.Builder(deviceEquipment).withLocation(location).build();
            client.execute(storeOp);
            return deviceEquipment;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
