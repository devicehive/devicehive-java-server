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
import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.dao.DeviceDao;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.riak.model.NetworkDevice;
import com.devicehive.dao.riak.model.RiakDevice;
import com.devicehive.dao.riak.model.RiakNetwork;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class DeviceDaoRiakImpl extends RiakGenericDao implements DeviceDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceDaoRiakImpl.class);

    private static final Namespace DEVICE_NS = new Namespace("device");
    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "dh_counters"),
            "deviceCounter");

    @Autowired
    private NetworkDao networkDao;

    @Autowired
    private DeviceClassDao deviceClassDao;

    @Autowired
    private UserNetworkDaoRiakImpl userNetworkDao;

    @Autowired
    private NetworkDeviceDaoRiakImpl networkDeviceDao;

    public DeviceDaoRiakImpl() {
    }

    @PostConstruct
    public void init() {
        ((NetworkDaoRiakImpl) networkDao).setDeviceDao(this);
    }

    /**
     * Method change statuses for devices with guids that consists in the list
     *
     * @param status new status
     * @param guids list of guids
     */
    @Override
    public void changeStatusForDevices(String status, List<String> guids) {
        for (String guid : guids) {
            DeviceVO device = findByUUID(guid);
            if (device != null) {
                device.setStatus(status);
                persist(device);
            }
        }
    }

    /**
     * Method return a Map where KEY is a device guid from guids list and VALUE
     * is OfflineTimeout from deviceClass for device with current guid.
     *
     * @param guids list of guids
     */
    @Override
    public Map<String, Integer> getOfflineTimeForDevices(List<String> guids) {
        final Map<String, Integer> deviceInfo = new HashMap<>();
        for (String guid : guids) {
            DeviceVO device = findByUUID(guid);
            if (device != null && device.getDeviceClass() != null) {
                refreshRefs(device);
                deviceInfo.put(guid, device.getDeviceClass().getOfflineTimeout());
            }
        }
        return deviceInfo;
    }

    @Override
    public DeviceVO findByUUID(String uuid) {
        BinIndexQuery biq = new BinIndexQuery.Builder(DEVICE_NS, "guid", uuid).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return null;
            }
            Location location = entries.get(0).getRiakObjectLocation();
            FetchValue fetchOp = new FetchValue.Builder(location)
                    .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                    .build();
            RiakDevice device = getOrNull(client.execute(fetchOp), RiakDevice.class);
            //TODO [rafa] refreshRefs
            DeviceVO deviceVO = RiakDevice.convertToVo(device);
//            deviceVO.setDeviceClass(device.getDeviceClass());
//            deviceVO.setNetwork(device.getNetwork());
            //TODO [rafa] do we need next refresh commands? Seems that all references are reconstructed.
            refreshRefs(deviceVO);
            return deviceVO;
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot find device by UUID.", e);
        }
    }

    @Override
    public void persist(DeviceVO vo) {
        RiakDevice device = RiakDevice.convertToEntity(vo);
        try {
            if (device.getId() == null) {
                device.setId(getId());
            }
            if (device.getDeviceClass() != null && device.getDeviceClass().getEquipment() != null) {
                device.getDeviceClass().getEquipment().clear();
            }
            Location location = new Location(DEVICE_NS, String.valueOf(device.getId()));
            StoreValue storeOp = new StoreValue.Builder(device)
                    .withLocation(location)
                    .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                    .build();
            client.execute(storeOp);
            vo.setId(device.getId());
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot persist device.", e);
        }

        RiakNetwork network = device.getNetwork();
        if (network != null && network.getId() != null) {
            LOGGER.debug("Creating relation between network[{}] and device[{}]", network.getId(), device.getGuid());
            networkDeviceDao.saveOrUpdate(new NetworkDevice(network.getId(), device.getGuid()));
            LOGGER.debug("Creating relation finished between network[{}] and device[{}]", network.getId(), device.getGuid());
        }
    }

    @Override
    public DeviceVO merge(DeviceVO device) {
        persist(device);
        return device;
    }

    @Override
    public int deleteByUUID(String guid) {
        try {
            BinIndexQuery biq = new BinIndexQuery.Builder(DEVICE_NS, "guid", guid).build();
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return 0;
            }
            Location location = entries.get(0).getRiakObjectLocation();
            DeleteValue deleteOp = new DeleteValue.Builder(location).build();
            client.execute(deleteOp);
            return 1;
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot delete device by UUID.", e);
        }
    }

    @Override
    public List<DeviceVO> getDeviceList(List<String> guids, HivePrincipal principal) {
        if (guids.isEmpty()) {
            return list(null, null, null, null, null,
                    null, null, null,
                    true, null,
                    null, principal);
        }
        List<DeviceVO> deviceList = guids.stream().map(this::findByUUID).collect(Collectors.toList());

        if (principal != null) {
            UserVO user = principal.getUser();

            if (user != null && !user.isAdmin()) {
                Set<Long> networks = userNetworkDao.findNetworksForUser(user.getId());
                deviceList = deviceList
                        .stream()
                        .filter(d -> networks.contains(d.getNetwork().getId()))
                        .collect(Collectors.toList());
            }

            if (principal.getDeviceGuids() != null) {
                deviceList = deviceList.stream()
                        .filter(d -> principal.getDeviceGuids().contains(d.getGuid()))
                        .collect(Collectors.toList());
            }

            if (principal.getNetworkIds() != null) {
                deviceList = deviceList.stream()
                        .filter(d -> principal.getNetworkIds().contains(d.getNetwork().getId()))
                        .collect(Collectors.toList());
            }
        }

        return deviceList;
    }

    @Override
    public long getAllowedDeviceCount(HivePrincipal principal, List<String> guids) {
        return getDeviceList(guids, principal).size();
    }

    @Override
    public List<DeviceVO> list(String name, String namePattern, String status, Long networkId, String networkName,
            Long deviceClassId, String deviceClassName, String sortField,
            Boolean isSortOrderAsc, Integer take,
            Integer skip, HivePrincipal principal) {
        //TODO [rafa] when filtering by device class name we have to instead query DeviceClass bucket for ids, and then use ids.
        // here is what happens, since device class is not embeddable in case of Riak we need to either keep id only and perform the logic above.
        // or we need to update device class embedded data in every device corresponding to the class, which is nighmare.

        BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                .withNamespace(DEVICE_NS);
        addMapValues(builder);
        if (name != null) {
            addReduceFilter(builder, "name", FilterOperator.EQUAL, name);
        } else if (namePattern != null) {
            namePattern = namePattern.replace("%", "");
            addReduceFilter(builder, "name", FilterOperator.REGEX, namePattern);
        }
        addReduceFilter(builder, "status", FilterOperator.EQUAL, status);
        addReduceFilter(builder, "network.id", FilterOperator.EQUAL, networkId);
        addReduceFilter(builder, "network.name", FilterOperator.EQUAL, networkName);
        addReduceFilter(builder, "deviceClass.id", FilterOperator.EQUAL, deviceClassId);
        addReduceFilter(builder, "deviceClass.name", FilterOperator.EQUAL, deviceClassName);
        if (principal != null) {
            UserVO user = principal.getUser();
            if (user != null && !user.isAdmin()) {
                Set<Long> networks = userNetworkDao.findNetworksForUser(user.getId());
                if (principal.getNetworkIds() != null) {
                    networks.retainAll(principal.getNetworkIds());
                }
                addReduceFilter(builder, "network.id", FilterOperator.IN, networks);
            }
            if (principal.getDeviceGuids() != null) {
                Set<String> deviceGuids = principal.getDeviceGuids();
                addReduceFilter(builder, "guid", FilterOperator.IN, deviceGuids);
            }
        }
        addReduceSort(builder, sortField, isSortOrderAsc);
        addReducePaging(builder, true, take, skip);
        try {
            MapReduce.Response response = client.execute(builder.build());
            return response.getResultsFromAllPhases(RiakDevice.class).stream()
                    .map(RiakDevice::convertToVo).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot get list of devices.", e);
        }
    }

    private DeviceVO refreshRefs(DeviceVO device) {
        if (device != null) {
            if (device.getNetwork() != null) {
                // todo: remove when migrate Device->DeviceVO
                NetworkVO networkVO = networkDao.find(device.getNetwork().getId());
                device.setNetwork(networkVO);
            }

            if (device.getDeviceClass() != null) {
                DeviceClassWithEquipmentVO deviceClassWithEquipmentVO = deviceClassDao.find(device.getDeviceClass().getId());
                device.setDeviceClass(deviceClassWithEquipmentVO);
            }
        }

        return device;
    }

    private Long getId() {
        return getId(COUNTERS_LOCATION);
    }
}
