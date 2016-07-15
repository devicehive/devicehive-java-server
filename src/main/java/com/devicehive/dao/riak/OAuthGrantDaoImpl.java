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
import com.devicehive.dao.AccessKeyDao;
import com.devicehive.dao.OAuthClientDao;
import com.devicehive.dao.OAuthGrantDao;
import com.devicehive.dao.UserDao;
import com.devicehive.model.AccessKey;
import com.devicehive.model.OAuthClient;
import com.devicehive.model.OAuthGrant;
import com.devicehive.model.User;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.enums.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Profile({"riak"})
@Repository
public class OAuthGrantDaoImpl implements OAuthGrantDao {

    private static final Namespace COUNTER_NS = new Namespace("counters", "oauth_grant_counters");
    private static final Namespace OAUTH_GRANT_NS = new Namespace("oauth_grant");

    @Autowired
    private RiakClient client;

    @Autowired
    private OAuthClientDao oAuthClientDao;

    @Autowired
    private AccessKeyDao accessKeyDao;

    @Autowired
    private UserDao userDao;

    private Location oauthGrantCounters;

    private final Map<String, String> sortMap = new HashMap<>();

    public OAuthGrantDaoImpl() {
        oauthGrantCounters = new Location(COUNTER_NS, "oauth_grant_counter");

        sortMap.put("timestamp", "function(a,b){ return a.timestamp %s b.timestamp; }");
        sortMap.put("authCode", "function(a,b){ return a.authCode %s b.authCode; }");
        sortMap.put("client", "function(a,b){ return a.client.name %s b.client.name; }");
        sortMap.put("accessKey", "function(a,b){ return a.accessKey.label %s b.accessKey.label; }");
        sortMap.put("user", "function(a,b){ return a.user.login %s b.user.login; }");
        sortMap.put("type", "function(a,b){ return a.type %s b.type; }");
        sortMap.put("accessType", "function(a,b){ return a.accessType %s b.accessType; }");
        sortMap.put("redirectUri", "function(a,b){ return a.redirectUri %s b.redirectUri; }");
        sortMap.put("scope", "function(a,b){ return a.scope %s b.scope; }");
        sortMap.put("entityVersion", "function(a,b){ return a.entityVersion %s b.entityVersion; }");
    }

    @Override
    public OAuthGrant getByIdAndUser(User user, Long grantId) {
        OAuthGrant grant = getById(grantId);
        if (grant.getUser().equals(user)) {
            return grant;
        } else {
            return null;
        }
    }

    @Override
    public OAuthGrant getById(Long grantId) {
        OAuthGrant grant = find(grantId);
        grant = updateRefs(grant);
        return grant;
    }

    private OAuthGrant updateRefs(OAuthGrant grant) {
        if (grant.getClient() != null) {
            OAuthClient client = oAuthClientDao.find(grant.getClient().getId());
            grant.setClient(client);
        }

        if (grant.getAccessKey() != null) {
            AccessKey key = accessKeyDao.find(grant.getAccessKey().getId());
            grant.setAccessKey(key);
        }

        if (grant.getUser() != null) {
            User user = userDao.find(grant.getUser().getId());
            grant.setUser(user);
        }
        return grant;
    }

    @Override
    public int deleteByUserAndId(User user, Long grantId) {
        try {
            Location location = new Location(OAUTH_GRANT_NS, String.valueOf(grantId));

            FetchValue fetchOp = new FetchValue.Builder(location).build();
            OAuthGrant grant = client.execute(fetchOp).getValue(OAuthGrant.class);

            if (grant.getUser().equals(user)) {
                DeleteValue deleteOp = new DeleteValue.Builder(location).build();
                client.execute(deleteOp);
                return 1;
            } else {
                return 0;
            }

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public OAuthGrant getByCodeAndOAuthID(String authCode, String clientOAuthID) {
        BinIndexQuery biq = new BinIndexQuery.Builder(OAUTH_GRANT_NS, "authCode", authCode).build();
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
                OAuthGrant oAuthGrant = client.execute(fetchOp).getValue(OAuthGrant.class);
                if (oAuthGrant.getClient() != null) {
                    OAuthClient client = oAuthClientDao.find(oAuthGrant.getClient().getId());
                    if (client.getOauthId().equals(clientOAuthID)) {
                        return oAuthGrant;
                    }
                }
            }

            return null;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public OAuthGrant find(Long id) {
        try {
            Location location = new Location(OAUTH_GRANT_NS, String.valueOf(id));
            FetchValue fetchOp = new FetchValue.Builder(location).build();
            return client.execute(fetchOp).getValue(OAuthGrant.class);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void persist(OAuthGrant oAuthGrant) {
        merge(oAuthGrant);
    }

    @Override
    public OAuthGrant merge(OAuthGrant oAuthGrant) {
        try {
            if (oAuthGrant.getId() == null) {
                CounterUpdate cu = new CounterUpdate(1);
                UpdateCounter update = new UpdateCounter.Builder(oauthGrantCounters, cu).build();
                client.execute(update);
                FetchCounter fetch = new FetchCounter.Builder(oauthGrantCounters).build();
                Long id = client.execute(fetch).getDatatype().view();
                oAuthGrant.setId(id);
            }
            Location location = new Location(OAUTH_GRANT_NS, String.valueOf(oAuthGrant.getId()));
            StoreValue storeOp = new StoreValue.Builder(oAuthGrant).withLocation(location).build();
            client.execute(storeOp);
            return oAuthGrant;
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<OAuthGrant> list(@NotNull User user,
                                 Date start,
                                 Date end,
                                 String clientOAuthId,
                                 Integer type,
                                 String scope,
                                 String redirectUri,
                                 Integer accessType,
                                 String sortField,
                                 Boolean sortOrder,
                                 Integer take,
                                 Integer skip) {
        ArrayList<OAuthGrant> result = new ArrayList<>();

        try {
            String sortFunction = sortMap.get(sortField);
            if (sortFunction == null) {
                sortFunction = sortMap.get("timestamp");
            }
            if (sortOrder == null) {
                sortOrder = true;
            }
            BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                    .withNamespace(OAUTH_GRANT_NS)
                    .withMapPhase(Function.newNamedJsFunction("Riak.mapValuesJson"));

            if (user != null) {
                long userId = user.getId();
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var id = v.user.id;" +
                                "return id == %s;" +
                                "})" +
                                "}", userId);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
            }

            if (start != null) {
                String functionString =
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var timestamp = v.timestamp;" +
                                "return timestamp.getTime() > arg.getTime();" +
                                "})" +
                                "}";
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction, start);
            }

            if (end != null) {
                String functionString =
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var timestamp = v.timestamp;" +
                                "return timestamp.getTime() < arg.getTime();" +
                                "})" +
                                "}";
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction, end);
            }

            if (clientOAuthId != null) {
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var oauthId = v.client.oauthId;" +
                                "return oauthId == '%s';" +
                                "})" +
                                "}", clientOAuthId);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
            }

            if (type != null) {
                String statusString = Type.getValueForIndex(type).toString();
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var type = v.type;" +
                                "return type == '%s';" +
                                "})" +
                                "}", statusString);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
            }

            if (scope != null) {
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var scope = v.scope;" +
                                "return scope == '%s';" +
                                "})" +
                                "}", scope);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
            }

            if (redirectUri != null) {
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var uri = v.redirectUri;" +
                                "return uri == '%s';" +
                                "})" +
                                "}", scope);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
            }

            if (accessType != null) {
                String statusString = AccessType.getValueForIndex(accessType).toString();
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var type = v.accessType;" +
                                "return type == '%s';" +
                                "})" +
                                "}", statusString);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
            }

            builder.withReducePhase(Function.newNamedJsFunction("Riak.reduceSort"),
                    String.format(sortFunction, sortOrder ? ">" : "<"),
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
            result.addAll(response.getResultsFromAllPhases(OAuthGrant.class));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return result;
    }
}
