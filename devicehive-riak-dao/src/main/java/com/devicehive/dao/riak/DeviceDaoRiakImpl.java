package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.api.commands.mapreduce.MapReduce;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.functions.Function;
import com.basho.riak.client.core.util.BinaryValue;
import com.devicehive.application.RiakQuorum;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.dao.DeviceDao;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.filter.AccessKeyBasedFilterForDevices;
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
import javax.validation.constraints.NotNull;
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
    private RiakClient client;

    @Autowired
    private NetworkDao networkDao;

    @Autowired
    private DeviceClassDao deviceClassDao;

    @Autowired
    private UserNetworkDaoRiakImpl userNetworkDao;

    @Autowired
    private NetworkDeviceDaoRiakImpl networkDeviceDao;

    @Autowired
    private RiakQuorum quorum;

    private final Map<String, String> sortMap = new HashMap<>();

    public DeviceDaoRiakImpl() {
        sortMap.put("id", "function(a,b){ return a.id %s b.id; }");
        sortMap.put("name", "function(a,b){ return a.name %s b.name; }");
        sortMap.put("guid", "function(a,b){ return a.guid %s b.guid; }");
        sortMap.put("status", "function(a,b){ return a.status %s b.status; }");
        sortMap.put("network", "function(a,b){ return a.network.name %s b.network.name; }");
        sortMap.put("deviceClass", "function(a,b){ return a.deviceClass.name %s b.deviceClass.name; }");
        sortMap.put("entityVersion", "function(a,b){ return a.entityVersion %s b.entityVersion; }");
    }

    @PostConstruct
    public void init() {
        ((NetworkDaoRiakImpl) networkDao).setDeviceDao(this);
    }

    /**
     * Method change statuses for devices with guids that consists in the list
     *
     * @param status new status
     * @param guids  list of guids
     */
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
     * Method return a Map where KEY is a device guid from guids list and
     * VALUE is OfflineTimeout from deviceClass for device with current guid.
     *
     * @param guids list of guids
     */
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
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot persist device.", e);
        }

        RiakNetwork network = device.getNetwork();
        if (network != null && network.getId() != null) {
            logger.debug("Creating relation between network[{}] and device[{}]", network.getId(), device.getGuid());
            networkDeviceDao.saveOrUpdate(new NetworkDevice(network.getId(), device.getGuid()));
            logger.debug("Creating relation finished between network[{}] and device[{}]", network.getId(), device.getGuid());
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
            logger.error("Exception accessing Riak Storage.", e);
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
            if (user == null && principal.getKey() != null) {
                user = principal.getKey().getUser();
            }

            if (user != null && !user.isAdmin()) {
                Set<Long> networks = userNetworkDao.findNetworksForUser(user.getId());
                deviceList = deviceList
                        .stream()
                        .filter(d -> networks.contains(d.getNetwork().getId()))
                        .collect(Collectors.toList());
            }

            if (principal.getKey() != null && principal.getKey().getPermissions() != null) {
                for (AccessKeyBasedFilterForDevices extraFilter :
                        AccessKeyBasedFilterForDevices.createExtraFilters(principal.getKey().getPermissions())) {
                    if (extraFilter.getDeviceGuids() != null) {
                        deviceList = deviceList.stream()
                                .filter(d -> extraFilter.getDeviceGuids().contains(d.getGuid()))
                                .collect(Collectors.toList());
                    }
                    if (extraFilter.getNetworkIds() != null) {
                        deviceList = deviceList.stream()
                                .filter(d -> extraFilter.getNetworkIds().contains(d.getNetwork().getId()))
                                .collect(Collectors.toList());
                    }
                }
            } else if (principal.getDevice() != null) {
                Long networkId = principal.getDevice().getNetwork().getId();
                deviceList = deviceList.stream()
                        .filter(d -> d.getNetwork().getId().equals(networkId))
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
                                @NotNull Boolean sortOrderAsc, Integer take,
                                Integer skip, HivePrincipal principal) {
        //TODO [rafa] when filtering by device class name we have to instead query DeviceClass bucket for ids, and then use ids.
        // here is what happens, since device class is not embeddable in case of Riak we need to either keep id only and perform the logic above.
        // or we need to update device class embedded data in every device corresponding to the class, which is nighmare.

        try {
            String sortFunction = sortMap.get(sortField);
            if (sortFunction == null) {
                sortFunction = sortMap.get("id");
            }
            if (sortOrderAsc == null) {
                sortOrderAsc = true;
            }
            BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                    .withNamespace(DEVICE_NS)
                    .withMapPhase(Function.newAnonymousJsFunction("function(riakObject, keyData, arg) { " +
                            "                if(riakObject.values[0].metadata['X-Riak-Deleted']){ return []; } " +
                            "                else { return Riak.mapValuesJson(riakObject, keyData, arg); }}"))
                    .withReducePhase(Function.newAnonymousJsFunction("function(values, arg) {" +
                            "return values.filter(function(v) {" +
                            "if (v === [] || v.name === null) { return false; }" +
                            "return true;" +
                            "})" +
                            "}"));

            if (name != null) {
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var name = v.name;" +
                                "return name == '%s';" +
                                "})" +
                                "}", name);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
            } else if (namePattern != null) {
                namePattern = namePattern.replace("%", "");
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var name = v.name;" +
                                "var match = name.indexOf('%s');" +
                                "return match > -1;" +
                                "})" +
                                "}", namePattern);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
            }

            if (status != null) {
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var status = v.status;" +
                                "return status == '%s';" +
                                "})" +
                                "}", status);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
            }

            if (networkId != null) {
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "if (v.network == null) return false;" +
                                "var networkId = v.network.id;" +
                                "return networkId == %s;" +
                                "})" +
                                "}", networkId);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
            }

            if (networkName != null) {
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "if (v.network == null) return false;" +
                                "var networkName = v.network.name;" +
                                "return networkName == '%s';" +
                                "})" +
                                "}", networkName);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
            }

            if (deviceClassId != null) {
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var deviceClassId = v.deviceClass.id;" +
                                "return deviceClassId == %s;" +
                                "})" +
                                "}", deviceClassId);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
            }

            if (deviceClassName != null) {
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var deviceClassName = v.deviceClass.name;" +
                                "return deviceClassName == '%s';" +
                                "})" +
                                "}", deviceClassName);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
            }

            if (principal != null && !principal.getRole().equals(HiveRoles.ADMIN)) {
                UserVO user = principal.getUser();
                if (user == null && principal.getKey() != null) {
                    user = principal.getKey().getUser();
                }

                if (user != null && !user.isAdmin()) {
                    Set<Long> networks = userNetworkDao.findNetworksForUser(user.getId());
                    String functionString =
                            "function(values, arg) {" +
                                    "return values.filter(function(v) {" +
                                    "if (v.network == null) return false;" +
                                    "var networkId = v.network.id;" +
                                    "return arg.indexOf(networkId) > -1;" +
                                    "})" +
                                    "}";
                    Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                    builder.withReducePhase(reduceFunction, networks);
                }

                if (principal.getKey() != null && principal.getKey().getPermissions() != null ) {
                    Set<AccessKeyPermissionVO> permissions = principal.getKey().getPermissions();
                    Set<String> deviceGuids = new HashSet<>();
                    for (AccessKeyPermissionVO permission : permissions) {
                        Set<String> guid = permission.getDeviceGuidsAsSet();
                        if (guid != null) {
                            deviceGuids.addAll(guid);
                        }
                    }
                    String functionString =
                            "function(values, arg) {" +
                                    "return values.filter(function(v) {" +
                                    "if (v.guid == null) return false;" +
                                    "var guid = v.guid;" +
                                    "return arg.indexOf(guid) > -1;" +
                                    "})" +
                                    "}";
                    Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                    if (!deviceGuids.isEmpty()) builder.withReducePhase(reduceFunction, deviceGuids);
                } else if (principal.getDevice() != null) {
                    String functionString = String.format(
                            "function(values, arg) {" +
                                    "return values.filter(function(v) {" +
                                    "var id = v.id;" +
                                    "return id == %s;" +
                                    "})" +
                                    "}", principal.getDevice().getId());
                    Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                    builder.withReducePhase(reduceFunction);
                }
            }
            builder.withReducePhase(Function.newNamedJsFunction("Riak.reduceSort"),
                    String.format(sortFunction, sortOrderAsc ? ">" : "<"),
                    true);

            if (take == null)
                take = Constants.DEFAULT_TAKE;
            if (skip == null)
                skip = 0;

            BucketMapReduce bmr = builder.build();
            RiakFuture<MapReduce.Response, BinaryValue> future = client.executeAsync(bmr);
            future.await();
            MapReduce.Response response = future.get();
            return response.getResultsFromAllPhases(RiakDevice.class).stream()
                    .skip(skip)
                    .limit(take)
                    .map(RiakDevice::convertToVo)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception accessing Riak Storage.", e);
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
