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

    private static final Logger logger = LoggerFactory.getLogger(DeviceDaoRiakImpl.class);

    private static final Namespace DEVICE_NS = new Namespace("device");
    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "dh_counters"),
            "deviceCounter");

    @Autowired
    private NetworkDao networkDao;

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

    @Override
    public DeviceVO findById(String id) {
        BinIndexQuery biq = new BinIndexQuery.Builder(DEVICE_NS, "device_id", id).build();
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
//            deviceVO.setNetwork(device.getNetwork());
            //TODO [rafa] do we need next refresh commands? Seems that all references are reconstructed.
            refreshRefs(deviceVO);
            return deviceVO;
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
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
            Location location = new Location(DEVICE_NS, String.valueOf(device.getId()));
            StoreValue storeOp = new StoreValue.Builder(device)
                    .withLocation(location)
                    .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                    .build();
            client.execute(storeOp);
            vo.setId(device.getId());
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot persist device.", e);
        }

        RiakNetwork network = device.getNetwork();
        if (network != null && network.getId() != null) {
            logger.debug("Creating relation between network[{}] and device[{}]", network.getId(), device.getDeviceId());
            networkDeviceDao.saveOrUpdate(new NetworkDevice(network.getId(), device.getDeviceId()));
            logger.debug("Creating relation finished between network[{}] and device[{}]", network.getId(), device.getDeviceId());
        }
    }

    @Override
    public DeviceVO merge(DeviceVO device) {
        persist(device);
        return device;
    }

    @Override
    public int deleteById(String id) {
        try {
            BinIndexQuery biq = new BinIndexQuery.Builder(DEVICE_NS, "device_id", id).build();
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
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot delete device by UUID.", e);
        }
    }

    @Override
    public List<DeviceVO> getDeviceList(List<String> deviceIds, HivePrincipal principal) {
        if (deviceIds.isEmpty()) {
            return list(null, null, null, null, null, true, null, null, principal);
        }
        List<DeviceVO> deviceList = deviceIds.stream().map(this::findById).collect(Collectors.toList());

        if (principal != null) {
            UserVO user = principal.getUser();

            if (user != null && !user.isAdmin()) {
                Set<Long> networks = userNetworkDao.findNetworksForUser(user.getId());
                deviceList = deviceList
                        .stream()
                        .filter(d -> networks.contains(d.getNetworkId()))
                        .collect(Collectors.toList());
            }

            if (principal.getDeviceIds() != null) {
                deviceList = deviceList.stream()
                        .filter(d -> principal.getDeviceIds().contains(d.getDeviceId()))
                        .collect(Collectors.toList());
            }

            if (principal.getNetworkIds() != null) {
                deviceList = deviceList.stream()
                        .filter(d -> principal.getNetworkIds().contains(d.getNetworkId()))
                        .collect(Collectors.toList());
            }
        }

        return deviceList;
    }

    @Override
    public List<DeviceVO> list(String name, String namePattern, Long networkId, String networkName,
            String sortField, boolean isSortOrderAsc, Integer take, Integer skip, HivePrincipal principal) {
        BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                .withNamespace(DEVICE_NS);
        addMapValues(builder);
        if (name != null) {
            addReduceFilter(builder, "name", FilterOperator.EQUAL, name);
        } else if (namePattern != null) {
            namePattern = namePattern.replace("%", "");
            addReduceFilter(builder, "name", FilterOperator.REGEX, namePattern);
        }
        addReduceFilter(builder, "network.id", FilterOperator.EQUAL, networkId);
        addReduceFilter(builder, "network.name", FilterOperator.EQUAL, networkName);
        if (principal != null) {
            UserVO user = principal.getUser();
            if (user != null && !user.isAdmin()) {
                Set<Long> networks = userNetworkDao.findNetworksForUser(user.getId());
                if (principal.getNetworkIds() != null) {
                    networks.retainAll(principal.getNetworkIds());
                }
                addReduceFilter(builder, "network.id", FilterOperator.IN, networks);
            }
            if (principal.getDeviceIds() != null) {
                Set<String> deviceIds = principal.getDeviceIds();
                addReduceFilter(builder, "device_id", FilterOperator.IN, deviceIds);
            }
        }
        addReduceSort(builder, sortField, isSortOrderAsc);
        addReducePaging(builder, true, take, skip);
        try {
            MapReduce.Response response = client.execute(builder.build());
            return response.getResultsFromAllPhases(RiakDevice.class).stream()
                    .map(RiakDevice::convertToVo).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot get list of devices.", e);
        }
    }

    private DeviceVO refreshRefs(DeviceVO device) {
        if (device != null) {
            if (device.getNetworkId() != null) {
                // todo: remove when migrate Device->DeviceVO
                NetworkVO networkVO = networkDao.find(device.getNetworkId());
                device.setNetworkId(networkVO.getId());
            }
        }

        return device;
    }

    private Long getId() {
        return getId(COUNTERS_LOCATION);
    }
}
