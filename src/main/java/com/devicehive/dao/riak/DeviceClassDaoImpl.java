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

    private Long getId() {
        return getId(COUNTERS_LOCATION);
    }

    @Override
    public void remove(DeviceClass reference) {
        try {
            Location location = new Location(DEVICE_CLASS_NS, String.valueOf(reference.getId()));
            DeleteValue delete = new DeleteValue.Builder(location).build();
            client.execute(delete);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DeviceClass find(Long id) {
        try {
            Location location = new Location(DEVICE_CLASS_NS, String.valueOf(id));
            FetchValue fetchOp = new FetchValue.Builder(location)
                    .build();
            return restoreEquipmentRefs(client.execute(fetchOp).getValue(DeviceClass.class));
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
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
                deviceClass.setId(getId());
            }
            Location location = new Location(DEVICE_CLASS_NS, String.valueOf(deviceClass.getId()));
            clearEquipmentRefs(deviceClass);
            StoreValue storeOp = new StoreValue.Builder(deviceClass)
                    .withLocation(location).build();
            client.execute(storeOp);
            return restoreEquipmentRefs(deviceClass);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
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
                        .withMapPhase(Function.newNamedJsFunction("Riak.mapValuesJson"))
                        .withReducePhase(Function.newNamedJsFunction("Riak.reduceSort"),
                                String.format(sortFunction, sortOrderAsc ? ">" : "<"), take == null && namePattern == null);
                if (namePattern != null) {
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
                RiakFuture<MapReduce.Response, BinaryValue> future = client.executeAsync(bmr);
                future.await();
                MapReduce.Response response = future.get();
                result.addAll(response.getResultsFromAllPhases(DeviceClass.class));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
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
                FetchValue fetchOp = new FetchValue.Builder(location)
                        .build();

                DeviceClass result = client.execute(fetchOp).getValue(DeviceClass.class);
                if (result != null) {
                    restoreEquipmentRefs(result);
                }
                return result;
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
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
        if (deviceClass.getEquipment() != null) {
            for (Equipment equipment : deviceClass.getEquipment()) {
                equipment.setDeviceClass(deviceClass);
            }
        }
        return deviceClass;
    }
}
