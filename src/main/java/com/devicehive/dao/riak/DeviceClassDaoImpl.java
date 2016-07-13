package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.RiakCommand;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.ListKeys;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.api.commands.mapreduce.MapReduce;
import com.basho.riak.client.api.commands.mapreduce.filters.MatchFilter;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.operations.SearchOperation;
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

import javax.persistence.LockModeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Profile({"riak"})
@Repository
public class DeviceClassDaoImpl implements DeviceClassDao {

    private static final Namespace DEVICE_CLASS_NS = new Namespace("deviceClass");

    @Autowired
    private RiakClient client;

    @Override
    public DeviceClass getReference(String name) {
        return find(name);
    }

    @Override
    public void remove(DeviceClass reference) {
        try {
            Location location = new Location(DEVICE_CLASS_NS, reference.getId());
            DeleteValue delete = new DeleteValue.Builder(location).build();
            client.execute(delete);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DeviceClass find(String id) {
        try {
            Location location = new Location(DEVICE_CLASS_NS, id);
            FetchValue fetchOp = new FetchValue.Builder(location)
                    .build();
            return client.execute(fetchOp).getValue(DeviceClass.class);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void refresh(DeviceClass stored, LockModeType lockModeType) {
        //do nothing...
    }

    @Override
    public void persist(DeviceClass deviceClass) {
        merge(deviceClass);
    }

    @Override
    public DeviceClass merge(DeviceClass deviceClass) {
        try {
            Location location = new Location(DEVICE_CLASS_NS, String.valueOf(deviceClass.getId()));
            clearEquipmentRefs(deviceClass);
            StoreValue storeOp = new StoreValue.Builder(deviceClass)
                    .withLocation(location).build();
            client.execute(storeOp);
            restoreEquipmentRefs(deviceClass);
            return deviceClass;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private final Map<String, String> sortMap = new HashMap<>();

    public DeviceClassDaoImpl() {
        sortMap.put("name", "function(a,b){ return a.id %s b.id; }");
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
            DeviceClass deviceClass = find(name);
            if (deviceClass != null) {
                result.add(deviceClass);
            }
        } else {
            try {
                String sortFunction = sortMap.get(sortField);
                if (sortFunction == null) {
                    sortFunction = sortMap.get("name");
                }
                BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                        .withNamespace(DEVICE_CLASS_NS)
                        .withMapPhase(Function.newNamedJsFunction("Riak.mapValuesJson"))
                        .withReducePhase(Function.newErlangFunction("Riak.reduceSlice",
                                String.format(sortFunction, sortOrderAsc ? ">" : "<")), take == null);
                if (namePattern != null) {
                    builder.withKeyFilter(new MatchFilter(namePattern));
                }
                if (take != null) {
                    int[] args = new int[2];
                    args[0] = skip != null ? skip : 0;
                    args[1] = args[0] + take;
                    builder.withReducePhase(Function.newNamedJsFunction("Riak.reduceSlice"), args, true);
                }
                BucketMapReduce bmr = builder.build();
                RiakFuture<MapReduce.Response, BinaryValue> future = client.executeAsync(bmr);
                future.await();
                MapReduce.Response response = future.get();
                result.addAll(response.getResultsFromAllPhases(DeviceClass.class));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    private void clearEquipmentRefs(DeviceClass deviceClass) {
        if (deviceClass.getEquipment() != null) {
            for (Equipment equipment : deviceClass.getEquipment()) {
                equipment.setDeviceClass(null);
            }
        }
    }

    private void restoreEquipmentRefs(DeviceClass deviceClass) {
        if (deviceClass.getEquipment() != null) {
            for (Equipment equipment : deviceClass.getEquipment()) {
                equipment.setDeviceClass(deviceClass);
            }
        }
    }
}
