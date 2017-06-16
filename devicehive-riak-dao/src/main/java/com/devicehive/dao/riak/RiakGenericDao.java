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
import com.devicehive.configuration.Constants;
import com.devicehive.exceptions.HivePersistenceLayerException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RiakGenericDao {

    protected static enum FilterOperator {
        EQUAL("="), MORE(">"), LESS("<"),
        MORE_EQUAL(">="), LESS_EQUAL("<="), NOT_EQUAL("!="),
        REGEX("regex"), IN("in"), CONTAINS("contains");
        private final String value;

        private FilterOperator(String value) {
            this.value = value;
        }
    }

    protected static enum SortOrder {
        ASC("asc"), DESC("desc");
        private final String value;

        private SortOrder(String value) {
            this.value = value;
        }
    }

    @Autowired
    protected RiakClient client;

    @Autowired
    protected RiakQuorum quorum;

    private final String MAP_REDUCE_FUNCTIONS_MODULE = "dhmr";

    protected final Function REDUCE_SORT = Function.newErlangFunction(MAP_REDUCE_FUNCTIONS_MODULE, "reduce_sort");
    protected final Function REDUCE_PAGINATION = Function.newErlangFunction(MAP_REDUCE_FUNCTIONS_MODULE, "reduce_pagination_filter");
    protected final Function REDUCE_ADD_INDEX = Function.newErlangFunction(MAP_REDUCE_FUNCTIONS_MODULE, "reduce_add_index");
    protected final Function REDUCE_DELETE_INDEX = Function.newErlangFunction(MAP_REDUCE_FUNCTIONS_MODULE, "reduce_delete_index");
    protected final Function REDUCE_FILTER = Function.newErlangFunction(MAP_REDUCE_FUNCTIONS_MODULE, "reduce_filter");
    protected final Function MAP_VALUES = Function.newErlangFunction(MAP_REDUCE_FUNCTIONS_MODULE, "map_values");

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

    protected BucketMapReduce.Builder addReducePaging(BucketMapReduce.Builder builder, Boolean keep, Integer count, Integer skip) {
        if (skip == null) {
            skip = 0;
        }
        if (count == null) {
            count = Constants.DEFAULT_TAKE;
        }
        builder.withReducePhase(REDUCE_ADD_INDEX)
                .withReducePhase(REDUCE_PAGINATION, new Object[]{skip, count})
                .withReducePhase(REDUCE_DELETE_INDEX, true);
        return builder;
    }

    protected BucketMapReduce.Builder addReducePaging(BucketMapReduce.Builder builder, Integer take, Integer skip) {
        return addReducePaging(builder, true, take, skip);
    }

    protected BucketMapReduce.Builder addReduceFilter(BucketMapReduce.Builder builder, Boolean keep, String fieldName, FilterOperator operation, Object value) {
        if ((fieldName == null) || (operation == null) || (value == null)) {
            return builder;
        } else {
            return builder.withReducePhase(REDUCE_FILTER, new Object[]{fieldName, operation.value, value}, keep);
        }
    }

    protected BucketMapReduce.Builder addReduceFilter(BucketMapReduce.Builder builder, String fieldName, FilterOperator operation, Object value) {
        return addReduceFilter(builder, false, fieldName, operation, value);
    }

    protected BucketMapReduce.Builder addReduceSort(BucketMapReduce.Builder builder, Boolean keep, String sortField, SortOrder order) {
        if ((sortField == null) || (order == null)) {
            return builder;
        } else {
            return builder.withReducePhase(REDUCE_SORT, new Object[]{sortField, order.value}, keep);
        }
    }

    protected BucketMapReduce.Builder addReduceSort(BucketMapReduce.Builder builder, String sortField, SortOrder order) {
        return addReduceSort(builder, false, sortField, order);
    }

    protected BucketMapReduce.Builder addReduceSort(BucketMapReduce.Builder builder, Boolean keep, String sortField, boolean isSortOrderAsc) {
        SortOrder sortOrder = (isSortOrderAsc) ? SortOrder.ASC : SortOrder.DESC;
        if ((sortField == null) || (sortField.isEmpty())) {
            return addReduceSort(builder, keep, "id", sortOrder);
        } else {
            return addReduceSort(builder, keep, sortField, sortOrder);
        }
    }

    protected BucketMapReduce.Builder addReduceSort(BucketMapReduce.Builder builder, String sortField, boolean isSortOrderAsc) {
        return addReduceSort(builder, false, sortField, isSortOrderAsc);
    }

    protected BucketMapReduce.Builder addMapValues(BucketMapReduce.Builder builder, Boolean keep) {
        return builder.withMapPhase(MAP_VALUES, keep);
    }

    protected BucketMapReduce.Builder addMapValues(BucketMapReduce.Builder builder) {
        return addMapValues(builder, false);
    }

    protected <T> T getOrNull(FetchValue.Response response, Class<T> clazz) throws UnresolvedConflictException {
        if (response.hasValues()) {
            return response.getValue(clazz);
        }
        return null;
    }

    protected int deleteById(Long id, Namespace ns) throws ExecutionException, InterruptedException {
        Location location = new Location(ns, String.valueOf(id));
        DeleteValue deleteOp = new DeleteValue.Builder(location).build();
        client.execute(deleteOp);
        return 1;
    }

    protected <T> T findBySecondaryIndex(String indexName, String value,
            Namespace namespace, Class<T> clazz) {
        if ((indexName == null) || (value == null)) {
            return null;
        }
        BinIndexQuery biq = new BinIndexQuery.Builder(namespace, indexName, value).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            return fetchOne(response, clazz);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find by identity.", e);
        }
    }

    protected <T> List<T> findAllBySecondaryIndex(String indexName, String value,
            Namespace namespace, Class<T> clazz) {
        if ((indexName == null) || (value == null)) {
            return null;
        }
        BinIndexQuery biq = new BinIndexQuery.Builder(namespace, indexName, value).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            return fetchMultiple(response, clazz);
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find by identity.", e);
        }
    }

    protected <T, R> List<T> fetchMultiple(SecondaryIndexQuery.Response<R> response, Class<T> clazz)
            throws ExecutionException, InterruptedException {
        List<?> entries = response.getEntries();
        if (entries.isEmpty()) {
            return Collections.emptyList();
        } else {
            final List<Location> locations = entries.stream()
                    .map(entry -> ((SecondaryIndexQuery.Response.Entry<R>) entry).getRiakObjectLocation()).collect(Collectors.toList());
            return fetchMultipleByLocations(locations, clazz);
        }
    }

    protected <T, R> T fetchOne(SecondaryIndexQuery.Response<R> response, Class<T> clazz)
            throws ExecutionException, InterruptedException {
        List<?> entries = response.getEntries();
        if (entries.isEmpty()) {
            return null;
        } else {
            Location location = ((SecondaryIndexQuery.Response.Entry<R>) entries.get(0)).getRiakObjectLocation();
            return fetchByLocation(location, clazz);
        }
    }

    private <T> T fetchByLocation(Location location, Class<T> clazz) throws ExecutionException, InterruptedException {
        FetchValue fv = new FetchValue.Builder(location).build();
        FetchValue.Response response = client.execute(fv);
        return response.getValue(clazz);
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
