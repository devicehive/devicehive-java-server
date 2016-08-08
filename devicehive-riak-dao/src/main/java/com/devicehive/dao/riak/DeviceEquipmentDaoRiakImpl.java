package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.dao.DeviceEquipmentDao;
import com.devicehive.dao.riak.model.RiakDeviceEquipment;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.vo.DeviceEquipmentVO;
import com.devicehive.vo.DeviceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Profile({"riak"})
@Repository
public class DeviceEquipmentDaoRiakImpl extends RiakGenericDao implements DeviceEquipmentDao {

    private static final Namespace DEVICE_EQUIPMENT_NS = new Namespace("device_equipment");
    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "dh_counters"),
            "deviceEquipmentCounter");

    @Autowired
    private RiakClient client;

    @Autowired
    private RiakQuorum quorum;

    @Override
    public List<DeviceEquipmentVO> getByDevice(DeviceVO device) {
        BinIndexQuery biq = new BinIndexQuery.Builder(DEVICE_EQUIPMENT_NS, "device", device.getGuid()).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<RiakDeviceEquipment> ts = fetchMultiple(response, RiakDeviceEquipment.class);
            return RiakDeviceEquipment.convertToVo(ts);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot get device equipment by device.", e);
        }
    }

    @Override
    public DeviceEquipmentVO getByDeviceAndCode(@NotNull String code, @NotNull DeviceVO device) {
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
                RiakDeviceEquipment deviceEquipment = getOrNull(client.execute(fetchOp), RiakDeviceEquipment.class);
                if (deviceEquipment.getCode().equals(code)) {
                    return RiakDeviceEquipment.convertToVo(deviceEquipment);
                }
            }

            return null;
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot cannot get device equipment by device and code.", e);
        }
    }

    @Override
    public void persist(DeviceEquipmentVO deviceEquipment, DeviceVO device) {
        merge(deviceEquipment, device);
    }

    @Override
    public DeviceEquipmentVO merge(DeviceEquipmentVO entity, DeviceVO device) {
        RiakDeviceEquipment deviceEquipment = RiakDeviceEquipment.convertToEntity(entity);
        deviceEquipment.setDevice(device.getGuid());

        try {
            if (deviceEquipment.getId() == null) {
                deviceEquipment.setId(getId(COUNTERS_LOCATION));
            }
            Location location = new Location(DEVICE_EQUIPMENT_NS, String.valueOf(deviceEquipment.getId()));
            StoreValue storeOp = new StoreValue.Builder(deviceEquipment)
                    .withLocation(location)
                    .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                    .build();
            client.execute(storeOp);
            entity.setId(deviceEquipment.getId());
            return RiakDeviceEquipment.convertToVo(deviceEquipment);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot merge device equipment.", e);
        }
    }
}
