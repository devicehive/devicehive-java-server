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
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Profile({"riak"})
@Repository
public class DeviceClassDaoImpl extends RiakGenericDao implements DeviceClassDao {

    private static final Namespace DEVICE_CLASS_NS = new Namespace("deviceClass");
    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "check_counters"),
            "deviceClassCounter");

    @Autowired
    private RiakClient client;

    @Override
    public DeviceClass getReference(Long id) {
        return find(id);
    }

    @Override
    public void remove(DeviceClass reference) {
        try {
            Location location = new Location(DEVICE_CLASS_NS, String.valueOf(reference.getId()));
            DeleteValue delete = new DeleteValue.Builder(location).build();
            client.execute(delete);
        } catch (InterruptedException | ExecutionException e) {
            throw new HivePersistenceLayerException("Cannot remove device class.", e);
        }
    }

    @Override
    public DeviceClass find(Long id) {
        try {
            Location location = new Location(DEVICE_CLASS_NS, String.valueOf(id));
            FetchValue fetchOp = new FetchValue.Builder(location)
                    .build();
            return restoreEquipmentRefs(getOrNull(client.execute(fetchOp), DeviceClass.class));
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find device class by id.", e);
        }
    }

    @Override
    public void persist(DeviceClass deviceClass) {
        merge(deviceClass);
    }

    @Override
    public DeviceClass merge(DeviceClass deviceClass) {
        try {
            if (deviceClass.getId() == null) {
                deviceClass.setId(getId(COUNTERS_LOCATION));
            }
            if (deviceClass.getName() == null) {
                throw new HivePersistenceLayerException("DeviceClass name can not be null");
            }
            Location location = new Location(DEVICE_CLASS_NS, String.valueOf(deviceClass.getId()));
            clearEquipmentRefs(deviceClass);
            StoreValue storeOp = new StoreValue.Builder(deviceClass)
                    .withLocation(location).build();
            client.execute(storeOp);
            return restoreEquipmentRefs(deviceClass);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot merge device class.", e);
        }
    }

    private final Map<String, String> sortMap = new HashMap<>();

    public DeviceClassDaoImpl() {
        sortMap.put("name", "function(a,b){ return a.name %s b.name; }");
        sortMap.put("offlineTimeout", "function(a,b){ return a.offlineTimeout %s b.offlineTimeout; }");
        sortMap.put("offlineTimeout", "function(a,b){ return a.offlineTimeout %s b.offlineTimeout; }");
        sortMap.put("isPermanent", "function(a,b){ return a.isPermanent %s b.isPermanent; }");
        sortMap.put("entityVersion", "function(a,b){ return a.entityVersion %s b.entityVersion; }");
    }

    @Override
    public List<DeviceClass> getDeviceClassList(String name, String namePattern, String sortField,
                                                Boolean sortOrderAsc, Integer take, Integer skip) {

        ArrayList<DeviceClass> result = new ArrayList<>();
        if (name != null) {
            DeviceClass deviceClass = findByName(name);
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
                result.addAll(response.getResultsFromAllPhases(DeviceClass.class));
            } catch (InterruptedException | ExecutionException e) {
                throw new HivePersistenceLayerException("Cannot get device class list.", e);
            }
        }
        result.forEach(this::restoreEquipmentRefs);
        return result;
    }

    public DeviceClass findByName(String name) {
        BinIndexQuery biq = new BinIndexQuery.Builder(DEVICE_CLASS_NS, "name", name).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return null;
            } else {
                Location location = entries.get(0).getRiakObjectLocation();
                FetchValue fetchOp = new FetchValue.Builder(location).build();
                return restoreEquipmentRefs(getOrNull(client.execute(fetchOp), DeviceClass.class));
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find device class by name.", e);
        }
    }

    private void clearEquipmentRefs(DeviceClass deviceClass) {
        if (deviceClass.getEquipment() != null) {
            for (Equipment equipment : deviceClass.getEquipment()) {
                equipment.setDeviceClass(null);
            }
        }
    }

    private DeviceClass restoreEquipmentRefs(DeviceClass deviceClass) {
        if (deviceClass != null) {
            if (deviceClass.getEquipment() != null) {
                for (Equipment equipment : deviceClass.getEquipment()) {
                    equipment.setDeviceClass(deviceClass);
                }
            }
        }
        return deviceClass;
    }
}
