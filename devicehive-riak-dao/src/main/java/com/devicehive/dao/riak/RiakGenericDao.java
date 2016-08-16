package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.cap.Quorum;
import com.basho.riak.client.api.cap.UnresolvedConflictException;
import com.basho.riak.client.api.commands.datatypes.CounterUpdate;
import com.basho.riak.client.api.commands.datatypes.UpdateCounter;
import com.basho.riak.client.api.commands.datatypes.UpdateDatatype;
import com.basho.riak.client.api.commands.indexes.*;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.MultiFetch;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.functions.Function;
import com.devicehive.application.RiakQuorum;
import com.devicehive.exceptions.HivePersistenceLayerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class RiakGenericDao {

    @Autowired
    RiakClient client;

    @Autowired
    RiakQuorum quorum;

    protected Long getId(Location location) {
        return getId(location, 1);
    }

    protected Long getId(Location location, int count) {
        CounterUpdate cu = new CounterUpdate(count);
        UpdateCounter update = new UpdateCounter.Builder(location, cu)
                .withOption(UpdateDatatype.Option.PW, Quorum.allQuorum())
                .withReturnDatatype(true).build();
        UpdateCounter.Response response;
        try {
            response = client.execute(update);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException(String.format("Unable to generate id for %s", location), e);
        }
        return response.getDatatype().view();
    }

    protected BucketMapReduce.Builder addPaging(BucketMapReduce.Builder builder, Integer take, Integer skip) {
        if (take != null) {
            int[] args = new int[2];
            args[0] = skip != null ? skip : 0;
            args[1] = args[0] + take;
            return builder.withReducePhase(Function.newNamedJsFunction("Riak.reduceSlice"), args, true);
        } else {
            return builder;
        }
    }

    protected<T> T getOrNull(FetchValue.Response response, Class<T> clazz) throws UnresolvedConflictException {
        if (response.hasValues()) {
            return response.getValue(clazz);
        }
        return null;
    }

    protected int deleteById(Long id, Namespace ns) throws ExecutionException, InterruptedException{
        Location location = new Location(ns, String.valueOf(id));
        DeleteValue deleteOp = new DeleteValue.Builder(location).build();
        client.execute(deleteOp);
        return 1;
    }

    protected<T> List<T> fetchMultiple(BigIntIndexQuery.Response response, Class<T> clazz)
            throws ExecutionException, InterruptedException {
        List<BigIntIndexQuery.Response.Entry> entries = response.getEntries();
        if (entries.isEmpty()) {
            return Collections.emptyList();
        } else {
            final List<Location> locations = entries.stream()
                    .map(BigIntIndexQuery.Response.Entry::getRiakObjectLocation).collect(Collectors.toList());
            return fetchMultipleByLocations(locations, clazz);
        }
    }

    protected<T> List<T> fetchMultiple(BinIndexQuery.Response response, Class<T> clazz)
            throws ExecutionException, InterruptedException {
        List<BinIndexQuery.Response.Entry> entries = response.getEntries();
        if (entries.isEmpty()) {
            return Collections.emptyList();
        } else {
            final List<Location> locations = entries.stream()
                    .map(BinIndexQuery.Response.Entry::getRiakObjectLocation).collect(Collectors.toList());
            return fetchMultipleByLocations(locations, clazz);
        }
    }

    protected<T> List<T> fetchMultiple(IntIndexQuery.Response response, Class<T> clazz)
            throws ExecutionException, InterruptedException {
        List<IntIndexQuery.Response.Entry> entries = response.getEntries();
        if (entries.isEmpty()) {
            return Collections.emptyList();
        } else {
            final List<Location> locations = entries.stream()
                    .map(IntIndexQuery.Response.Entry::getRiakObjectLocation).collect(Collectors.toList());
            return fetchMultipleByLocations(locations, clazz);
        }
    }

    protected<T> List<T> fetchMultiple(RawIndexQuery.Response response, Class<T> clazz)
            throws ExecutionException, InterruptedException {
        List<RawIndexQuery.Response.Entry> entries = response.getEntries();
        if (entries.isEmpty()) {
            return Collections.emptyList();
        } else {
            final List<Location> locations = entries.stream()
                    .map(RawIndexQuery.Response.Entry::getRiakObjectLocation).collect(Collectors.toList());
            return fetchMultipleByLocations(locations, clazz);
        }
    }

    private <T> List<T> fetchMultipleByLocations(List<Location> locations, Class<T> clazz) throws ExecutionException, InterruptedException {
        List<T> result = new ArrayList<>();
        MultiFetch multiFetch = new MultiFetch.Builder()
                .addLocations(locations)
                .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                .build();
        MultiFetch.Response mfr = client.execute(multiFetch);
        for (RiakFuture<FetchValue.Response, Location> f : mfr.getResponses()) {
            FetchValue.Response resp = f.get();
            result.add(resp.getValue(clazz));
        }
        return result;
    }
}
