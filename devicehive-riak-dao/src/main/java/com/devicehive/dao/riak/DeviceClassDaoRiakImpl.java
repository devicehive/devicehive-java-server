package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.api.commands.mapreduce.MapReduce;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.functions.Function;
import com.devicehive.application.RiakQuorum;
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.dao.riak.model.RiakDeviceClass;
import com.devicehive.dao.riak.model.RiakDeviceClassEquipment;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class DeviceClassDaoRiakImpl extends RiakGenericDao implements DeviceClassDao {

    private static final Namespace DEVICE_CLASS_NS = new Namespace("device_class");

    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "dh_counters"),
            "deviceClassCounter");

    @Autowired
    private RiakClient client;

    @Autowired
    private RiakQuorum quorum;

    private final Map<String, String> sortMap = new HashMap<>();

    public DeviceClassDaoRiakImpl() {
        sortMap.put("name", "function(a,b){ return a.name %s b.name; }");
        sortMap.put("offlineTimeout", "function(a,b){ return a.offlineTimeout %s b.offlineTimeout; }");
        sortMap.put("offlineTimeout", "function(a,b){ return a.offlineTimeout %s b.offlineTimeout; }");
        sortMap.put("isPermanent", "function(a,b){ return a.isPermanent %s b.isPermanent; }");
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
    public List<DeviceClassWithEquipmentVO> getDeviceClassList(String name, String namePattern, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip) {

        List<DeviceClassWithEquipmentVO> result = new ArrayList<>();
        if (name != null) {
            DeviceClassWithEquipmentVO deviceClass = findByName(name);
            if (deviceClass != null)
                result.add(deviceClass);
        } else {
            try {
                String sortFunction = sortMap.get(sortField);
                if (sortFunction == null) {
                    sortFunction = sortMap.get("name");
                }
                BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                        .withNamespace(DEVICE_CLASS_NS)
                        .withMapPhase(Function.newAnonymousJsFunction("function(riakObject, keyData, arg) { " +
                        "                if(riakObject.values[0].metadata['X-Riak-Deleted']){ return []; } " +
                        "                else { return Riak.mapValuesJson(riakObject, keyData, arg); }}"))
                        .withReducePhase(Function.newAnonymousJsFunction("function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "if (v === [] || v.name === null) { return false; }" +
                                "return true;" +
                                "})" +
                                "}"))
                        .withReducePhase(Function.newNamedJsFunction("Riak.reduceSort"),
                                String.format(sortFunction, sortOrderAsc ? ">" : "<"), take == null && namePattern == null);
                if (namePattern != null) {
                    if (namePattern.startsWith("%")) {
                        namePattern = namePattern.substring(1);
                    }
                    if (namePattern.endsWith("%")) {
                        namePattern = namePattern.substring(0, namePattern.length() - 1);
                    }
                    String functionBody = String.format(
                            "function(values, arg) {" +
                                    "  return values.filter(function(v) {" +
                                    "    return v.name.indexOf('%s') > -1;" +
                                    "  })" +
                                    "}", namePattern);
                    builder = builder.withReducePhase(Function.newAnonymousJsFunction(functionBody), take == null);
                }
                builder = addPaging(builder, take, skip);
                BucketMapReduce bmr = builder.build();
                MapReduce.Response response = client.execute(bmr);
                Collection<RiakDeviceClass> resultsFromAllPhases = response.getResultsFromAllPhases(RiakDeviceClass.class);
                Stream<DeviceClassWithEquipmentVO> objectStream = resultsFromAllPhases.stream().map(RiakDeviceClass::convertDeviceClassWithEquipment);
                List<DeviceClassWithEquipmentVO> voList = objectStream.collect(Collectors.toList());
                result.addAll(voList);
            } catch (InterruptedException | ExecutionException e) {
                throw new HivePersistenceLayerException("Cannot get device class list.", e);
            }
        }
        return result;
    }

    @Override
    public DeviceClassWithEquipmentVO findByName(@NotNull String name) {
        if (name == null) {
            return null;
        }
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
