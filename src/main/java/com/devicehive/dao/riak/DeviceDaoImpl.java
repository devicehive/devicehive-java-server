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
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.configuration.Constants;
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.dao.DeviceDao;
import com.devicehive.dao.NetworkDao;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Network;
import com.devicehive.model.NetworkDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Profile({"riak"})
@Repository
public class DeviceDaoImpl extends RiakGenericDao implements DeviceDao {

    private static final Logger logger = LoggerFactory.getLogger(DeviceDaoImpl.class);

    private static final Namespace DEVICE_NS = new Namespace("device");
    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "device_counters"),
            "device_counter");

    @Autowired
    private RiakClient client;

    @Autowired
    private NetworkDao networkDao;

    @Autowired
    private DeviceClassDao deviceClassDao;

    @Autowired
    private UserNetworkDaoImpl userNetworkDao;

    @Autowired
    private NetworkDeviceDaoImpl networkDeviceDao;

    private final Map<String, String> sortMap = new HashMap<>();

    public DeviceDaoImpl() {
        sortMap.put("name", "function(a,b){ return a.name %s b.name; }");
        sortMap.put("guid", "function(a,b){ return a.guid %s b.guid; }");
        sortMap.put("status", "function(a,b){ return a.status %s b.status; }");
        sortMap.put("network", "function(a,b){ return a.network.name %s b.network.name; }");
        sortMap.put("deviceClass", "function(a,b){ return a.deviceClass.name %s b.deviceClass.name; }");
        sortMap.put("entityVersion", "function(a,b){ return a.entityVersion %s b.entityVersion; }");
    }

    @PostConstruct
    public void init() {
        ((NetworkDaoImpl) networkDao).setDeviceDao(this);
    }

    /**
     * Method change statuses for devices with guids that consists in the list
     *
     * @param status new status
     * @param guids  list of guids
     */
    public void changeStatusForDevices(String status, List<String> guids) {
        for (String guid : guids) {
            Device device = findByUUID(guid);
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
            Device device = findByUUID(guid);
            if (device != null) {
                refreshRefs(device);
                deviceInfo.put(guid, device.getDeviceClass().getOfflineTimeout());
            }
        }
        return deviceInfo;
    }


    @Override
    public Device findByUUID(String uuid) {
        BinIndexQuery biq = new BinIndexQuery.Builder(DEVICE_NS, "guid", uuid).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return null;
            }
            Location location = entries.get(0).getRiakObjectLocation();
            FetchValue fetchOp = new FetchValue.Builder(location).build();
            Device device = getOrNull(client.execute(fetchOp), Device.class);
            return refreshRefs(device);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot find device by UUID.", e);
        }
    }

    @Override
    public void persist(Device device) {
        try {
            if (device.getId() == null) {
                device.setId(getId());
            }
            Location location = new Location(DEVICE_NS, String.valueOf(device.getId()));
            StoreValue storeOp = new StoreValue.Builder(device).withLocation(location).build();
            client.execute(storeOp);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot persist device.", e);
        }

        Network network = device.getNetwork();
        if (network != null && network.getId() != null) {
            logger.debug("Creating relation between network[{}] and device[{}]", network.getId(), device.getGuid());
            networkDeviceDao.saveOrUpdate(new NetworkDevice(network.getId(), device.getGuid()));
        }
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
    public List<Device> getDeviceList(List<String> guids, HivePrincipal principal) {
        List<Device> deviceList = new ArrayList<>();

        if (principal == null || principal.getRole().equals(HiveRoles.ADMIN)) {
            for (String guid : guids) {
                Device device = findByUUID(guid);
                if (device != null) {
                    deviceList.add(device);
                }
            }
        } else {
            try {
                BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                        .withNamespace(DEVICE_NS)
                        .withMapPhase(Function.newNamedJsFunction("Riak.mapValuesJson"));

                String functionString =
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var guid = v.guid;" +
                                "return arg.indexOf(guid) > -1;" +
                                "})" +
                                "}";
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction, guids.toArray());

                BucketMapReduce bmr = builder.build();
                RiakFuture<MapReduce.Response, BinaryValue> future = client.executeAsync(bmr);
                future.await();
                MapReduce.Response response = future.get();
                deviceList.addAll(response.getResultsFromAllPhases(Device.class));

                if (principal.getUser() != null) {
                    Set<Long> networks = userNetworkDao.findNetworksForUser(principal.getUser().getId());
                    deviceList = deviceList
                            .stream()
                            .filter(d -> networks.contains(d.getNetwork().getId()))
                            .collect(Collectors.toList());
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Exception accessing Riak Storage.", e);
                throw new HivePersistenceLayerException("Cannot get list of devices for list of UUIDs and principal.", e);
            }
        }
        return deviceList;
    }

    @Override
    public long getAllowedDeviceCount(HivePrincipal principal, List<String> guids) {
        return getDeviceList(guids, principal).size();
    }

    @Override
    public List<Device> getList(String name, String namePattern, String status, Long networkId, String networkName,
                                Long deviceClassId, String deviceClassName, String sortField,
                                @NotNull Boolean sortOrderAsc, Integer take,
                                Integer skip, HivePrincipal principal) {
        List<Device> result = new ArrayList<>();

        try {
            String sortFunction = sortMap.get(sortField);
            if (sortFunction == null) {
                sortFunction = sortMap.get("name");
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
                Set<Long> networks = userNetworkDao.findNetworksForUser(principal.getUser().getId());

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

            Collection devices = response.getResultsFromAllPhases(Device.class);
            result.addAll(devices);

            result = result.stream()
                    .skip(skip)
                    .limit(take)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot get list of devices.", e);
        }
        return result;
    }

    private Device refreshRefs(Device device) {
        if (device != null) {
            if (device.getNetwork() != null) {
                Network network = networkDao.find(device.getNetwork().getId());
                device.setNetwork(network);
            }

            if (device.getDeviceClass() != null) {
                DeviceClass deviceClass = deviceClassDao.find(device.getDeviceClass().getId());
                device.setDeviceClass(deviceClass);
            }
        }

        return device;
    }

    private Long getId() {
        return getId(COUNTERS_LOCATION);
    }
}
