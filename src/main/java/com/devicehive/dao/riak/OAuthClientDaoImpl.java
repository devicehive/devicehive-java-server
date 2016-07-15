package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.datatypes.CounterUpdate;
import com.basho.riak.client.api.commands.datatypes.FetchCounter;
import com.basho.riak.client.api.commands.datatypes.UpdateCounter;
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
import com.devicehive.dao.OAuthClientDao;
import com.devicehive.model.OAuthClient;
import com.devicehive.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Profile({"riak"})
@Repository
public class OAuthClientDaoImpl implements OAuthClientDao {

    private static final Namespace COUNTER_NS = new Namespace("counters", "oauth_client_counters");
    private static final Namespace OAUTH_CLIENT_NS = new Namespace("oauth_client");

    @Autowired
    private RiakClient client;

    private Location oauthClientCounters;

    private final Map<String, String> sortMap = new HashMap<>();

    public OAuthClientDaoImpl() {
        oauthClientCounters = new Location(COUNTER_NS, "oauth_client_counter");

        sortMap.put("name", "function(a,b){ return a.name %s b.name; }");
        sortMap.put("domain", "function(a,b){ return a.domain %s b.domain; }");
        sortMap.put("subnet", "function(a,b){ return a.subnet %s b.subnet; }");
        sortMap.put("redirectUri", "function(a,b){ return a.redirectUri %s b.redirectUri; }");
        sortMap.put("oauthId", "function(a,b){ return a.oauthId %s b.oauthId; }");
        sortMap.put("oauthSecret", "function(a,b){ return a.oauthSecret %s b.oauthSecret; }");
        sortMap.put("entityVersion", "function(a,b){ return a.entityVersion %s b.entityVersion; }");
    }

    @Override
    public int deleteById(Long id) {
        Location location = new Location(OAUTH_CLIENT_NS, String.valueOf(id));
        DeleteValue deleteOp = new DeleteValue.Builder(location).build();
        try {
            client.execute(deleteOp);
            return 1;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public OAuthClient getByOAuthId(String oauthId) {
        return findBySomeIndex(oauthId, "oauthId");
    }

    @Override
    public OAuthClient getByName(String name) {
        return findBySomeIndex(name, "name");
    }

    @Override
    public OAuthClient getByOAuthIdAndSecret(String id, String secret) {
        BinIndexQuery biq = new BinIndexQuery.Builder(OAUTH_CLIENT_NS, "oauthId", id).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return null;
            }
            for (BinIndexQuery.Response.Entry e : entries) {
                Location location = e.getRiakObjectLocation();
                FetchValue fetchOp = new FetchValue.Builder(location)
                        .build();
                OAuthClient oAuthClient = client.execute(fetchOp).getValue(OAuthClient.class);
                if (oAuthClient.getOauthSecret().equals(secret)) {
                    return oAuthClient;
                }
            }

            return null;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private OAuthClient findBySomeIndex(String name, String indexName) {
        BinIndexQuery biq = new BinIndexQuery.Builder(OAUTH_CLIENT_NS, indexName, name).build();
        try {
            BinIndexQuery.Response response = client.execute(biq);
            List<BinIndexQuery.Response.Entry> entries = response.getEntries();
            if (entries.isEmpty()) {
                return null;
            }
            Location location = entries.get(0).getRiakObjectLocation();
            FetchValue fetchOp = new FetchValue.Builder(location)
                    .build();
            return client.execute(fetchOp).getValue(OAuthClient.class);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public OAuthClient find(Long id) {
        try {
            Location location = new Location(OAUTH_CLIENT_NS, String.valueOf(id));
            FetchValue fetchOp = new FetchValue.Builder(location)
                    .build();
            return client.execute(fetchOp).getValue(OAuthClient.class);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void persist(OAuthClient oAuthClient) {
        merge(oAuthClient);
    }

    @Override
    public OAuthClient merge(OAuthClient oAuthClient) {
        try {
            if (oAuthClient.getId() == null) {
                CounterUpdate cu = new CounterUpdate(1);
                UpdateCounter update = new UpdateCounter.Builder(oauthClientCounters, cu).build();
                client.execute(update);
                FetchCounter fetch = new FetchCounter.Builder(oauthClientCounters).build();
                Long id = client.execute(fetch).getDatatype().view();
                oAuthClient.setId(id);
            }
            Location location = new Location(OAUTH_CLIENT_NS, String.valueOf(oAuthClient.getId()));
            StoreValue storeOp = new StoreValue.Builder(oAuthClient).withLocation(location).build();
            client.execute(storeOp);
            return oAuthClient;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<OAuthClient> get(String name,
                                 String namePattern,
                                 String domain,
                                 String oauthId,
                                 String sortField,
                                 Boolean sortOrderAsc,
                                 Integer take,
                                 Integer skip) {
        ArrayList<OAuthClient> result = new ArrayList<>();
        if (name != null) {
            OAuthClient client = getByName(name);
            if (client != null) {
                result.add(client);
            }
        } else {
            try {
                String sortFunction = sortMap.get(sortField);
                if (sortFunction == null) {
                    sortFunction = sortMap.get("name");
                }
                if (sortOrderAsc == null) {
                    sortOrderAsc = true;
                }
                BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                        .withNamespace(OAUTH_CLIENT_NS)
                        .withMapPhase(Function.newNamedJsFunction("Riak.mapValuesJson"));

                if (namePattern != null) {
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

                if (domain != null) {
                    String functionString = String.format(
                            "function(values, arg) {" +
                                    "return values.filter(function(v) {" +
                                    "var domain = v.domain;" +
                                    "return role == '%s';" +
                                    "})" +
                                    "}", domain);
                    Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                    builder.withReducePhase(reduceFunction);
                }

                if (oauthId != null) {
                    String functionString = String.format(
                            "function(values, arg) {" +
                                    "return values.filter(function(v) {" +
                                    "var oauthId = v.oauthId;" +
                                    "return status == '%s';" +
                                    "})" +
                                    "}", oauthId);
                    Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                    builder.withReducePhase(reduceFunction);
                }

                builder.withReducePhase(Function.newNamedJsFunction("Riak.reduceSort"),
                        String.format(sortFunction, sortOrderAsc ? ">" : "<"),
                        take == null);

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
                result.addAll(response.getResultsFromAllPhases(OAuthClient.class));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
}
