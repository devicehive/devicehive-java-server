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
import com.devicehive.dao.DeviceEquipmentDao;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceEquipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Profile({"riak"})
@Repository
public class DeviceEquipmentDaoRiakImpl extends RiakGenericDao implements DeviceEquipmentDao {

    private static final Namespace COUNTER_NS = new Namespace("counters", "device_equipment_counters");
    private static final Namespace DEVICE_EQUIPMENT_NS = new Namespace("device_equipment");

    @Autowired
    private RiakClient client;

    @Autowired
    private RiakQuorum quorum;

    @Override
    public List<DeviceEquipment> getByDevice(Device device) {
        BinIndexQuery biq = new BinIndexQuery.Builder(DEVICE_EQUIPMENT_NS, "device", device.getGuid()).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            return fetchMultiple(response, DeviceEquipment.class);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot get device equipment by device.", e);
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
                        .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                        .build();
                DeviceEquipment deviceEquipment = getOrNull(client.execute(fetchOp), DeviceEquipment.class);
                if (deviceEquipment.getCode().equals(code)) {
                    return deviceEquipment;
                }
            }

            return null;
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot cannot get device equipment by device and code.", e);
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
                deviceEquipment.setId(getId(deviceEquipmentCounters));
            }
            Location location = new Location(DEVICE_EQUIPMENT_NS, String.valueOf(deviceEquipment.getId()));
            StoreValue storeOp = new StoreValue.Builder(deviceEquipment)
                    .withLocation(location)
                    .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                    .build();
            client.execute(storeOp);
            return deviceEquipment;
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot merge device equipment.", e);
        }
    }
}
