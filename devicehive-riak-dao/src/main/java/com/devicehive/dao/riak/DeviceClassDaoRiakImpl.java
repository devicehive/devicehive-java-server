package com.devicehive.dao.riak;

/*
 * #%L
 * DeviceHive Dao Riak Implementation
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.api.commands.mapreduce.MapReduce;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.dao.riak.model.RiakDeviceClass;
import com.devicehive.dao.riak.model.RiakDeviceClassEquipment;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class DeviceClassDaoRiakImpl extends RiakGenericDao implements DeviceClassDao {

    private static final Namespace DEVICE_CLASS_NS = new Namespace("device_class");

    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "dh_counters"),
            "deviceClassCounter");

    public DeviceClassDaoRiakImpl() {
    }

    @Override
    public void remove(long id) {
        try {
            Location location = new Location(DEVICE_CLASS_NS, String.valueOf(id));
            DeleteValue delete = new DeleteValue.Builder(location).build();
            client.execute(delete);
        } catch (InterruptedException | ExecutionException e) {
            throw new HivePersistenceLayerException("Cannot remove device class.", e);
        }
    }

    @Override
    public DeviceClassWithEquipmentVO find(long id) {
        try {
            RiakDeviceClass deviceClass = findEntityInStore(id);
            return RiakDeviceClass.convertDeviceClassWithEquipment(deviceClass);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find device class by id.", e);
        }
    }

    @Override
    public DeviceClassWithEquipmentVO persist(DeviceClassWithEquipmentVO deviceClass) {
        return merge(deviceClass);
    }

    @Override
    public DeviceClassWithEquipmentVO merge(DeviceClassWithEquipmentVO deviceClass) {
        if (deviceClass.getName() == null) {
            throw new HivePersistenceLayerException("DeviceClass name can not be null");
        }
        try {
            if (deviceClass.getId() == null) {
                Long deviceClassEntityId = getId(COUNTERS_LOCATION);
                deviceClass.setId(deviceClassEntityId);
            }

            if (deviceClass.getEquipment() != null) {
                Long id = getId(COUNTERS_LOCATION, deviceClass.getEquipment().size());
                for (DeviceClassEquipmentVO deviceClassEquipmentVO : deviceClass.getEquipment()) {
                    if (deviceClassEquipmentVO.getId() == null) {
                        deviceClassEquipmentVO.setId(id--);
                    }
                }
            }

            RiakDeviceClass riakDeviceClass = RiakDeviceClass.convertWithEquipmentToEntity(deviceClass);
            Location location = new Location(DEVICE_CLASS_NS, String.valueOf(deviceClass.getId()));
            StoreValue storeOp = new StoreValue.Builder(riakDeviceClass).withLocation(location)
                    .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                    .build();
            client.execute(storeOp);

            return deviceClass;
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot merge device class.", e);
        }
    }

    @Override
    public List<DeviceClassWithEquipmentVO> list(String name, String namePattern, String sortField,
            boolean isSortOrderAsc, Integer take, Integer skip) {
        BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                .withNamespace(DEVICE_CLASS_NS);
        addMapValues(builder);
        if (name != null) {
            addReduceFilter(builder, "name", FilterOperator.EQUAL, name);
        } else if (namePattern != null) {
            namePattern = namePattern.replace("%", "");
            addReduceFilter(builder, "name", FilterOperator.REGEX, namePattern);
        }
        addReduceSort(builder, sortField, isSortOrderAsc);
        addReducePaging(builder, true, take, skip);
        try {
            MapReduce.Response response = client.execute(builder.build());
            return response.getResultsFromAllPhases(RiakDeviceClass.class).stream()
                    .map(RiakDeviceClass::convertDeviceClassWithEquipment).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new HivePersistenceLayerException("Cannot get device class list.", e);
        }
    }

    @Override
    public DeviceClassWithEquipmentVO findByName(@NotNull String name) {
        BinIndexQuery biq = new BinIndexQuery.Builder(DEVICE_CLASS_NS, "name", name).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return null;
            } else {
                Location location = entries.get(0).getRiakObjectLocation();
                FetchValue fetchOp = new FetchValue.Builder(location)
                        .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                        .build();
                RiakDeviceClass deviceClass = getOrNull(client.execute(fetchOp), RiakDeviceClass.class);
                return RiakDeviceClass.convertDeviceClassWithEquipment(deviceClass);
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find device class by name.", e);
        }
    }

    @Override
    public DeviceClassEquipmentVO findDeviceClassEquipment(@NotNull long deviceClassId, @NotNull long equipmentId) {
        try {
            RiakDeviceClass deviceClass = findEntityInStore(deviceClassId);
            if (deviceClass.getEquipment() != null) {
                for (RiakDeviceClassEquipment equipment : deviceClass.getEquipment()) {
                    if (equipment.getId() == equipmentId) {
                        return RiakDeviceClassEquipment.convertDeviceClassEquipment(equipment);
                    }
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find device class equipment by id.", e);
        }
        return null;
    }

    private RiakDeviceClass findEntityInStore(long id) throws ExecutionException, InterruptedException {
        Location location = new Location(DEVICE_CLASS_NS, String.valueOf(id));
        FetchValue fetchOp = new FetchValue.Builder(location)
                .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                .build();
        return getOrNull(client.execute(fetchOp), RiakDeviceClass.class);
    }
}
