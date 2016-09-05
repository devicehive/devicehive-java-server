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
import com.devicehive.configuration.Constants;
import com.devicehive.dao.OAuthClientDao;
import com.devicehive.dao.riak.model.RiakOAuthClient;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.vo.OAuthClientVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class OAuthClientDaoRiakImpl extends RiakGenericDao implements OAuthClientDao {

    private static final Logger logger = LoggerFactory.getLogger(OAuthClientDaoRiakImpl.class);

    private static final Namespace OAUTH_CLIENT_NS = new Namespace("oauth_client");

    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "dh_counters"),
            "oauthClientCounter");

    @Autowired
    private RiakClient client;

    @Autowired
    private RiakQuorum quorum;

    private final Map<String, String> sortMap = new HashMap<>();

    public OAuthClientDaoRiakImpl() {
        sortMap.put("id", "function(a,b){ return a.id %s b.id; }");
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
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot delete OAuthClient by id.", e);
        }
    }

    @Override
    public OAuthClientVO getByOAuthId(String oauthId) {
        return findBySomeIndex(oauthId, "oauthId");
    }

    @Override
    public OAuthClientVO getByName(String name) {
        return findBySomeIndex(name, "name");
    }

    @Override
    public OAuthClientVO getByOAuthIdAndSecret(String id, String secret) {
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
                        .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                        .build();
                RiakOAuthClient oAuthClient = getOrNull(client.execute(fetchOp), RiakOAuthClient.class);
                if (oAuthClient.getOauthSecret().equals(secret)) {
                    return RiakOAuthClient.convert(oAuthClient);
                }
            }
            return null;
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot fetch OAuthClient by id and secret.", e);
        }
    }

    private OAuthClientVO findBySomeIndex(String name, String indexName) {
        if (name == null || indexName == null) {
            return null;
        }
        BinIndexQuery biq = new BinIndexQuery.Builder(OAUTH_CLIENT_NS, indexName, name).build();
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
            FetchValue.Response execute = client.execute(fetchOp);
            return RiakOAuthClient.convert(getOrNull(execute, RiakOAuthClient.class));
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot delete OAuthClient by index.", e);
        }
    }

    @Override
    public OAuthClientVO find(Long id) {
        try {
            Location location = new Location(OAUTH_CLIENT_NS, String.valueOf(id));
            FetchValue fetchOp = new FetchValue.Builder(location)
                    .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                    .build();
            FetchValue.Response execute = client.execute(fetchOp);
            return RiakOAuthClient.convert(getOrNull(execute, RiakOAuthClient.class));
        } catch (ExecutionException | InterruptedException e) {
            throw new HivePersistenceLayerException("Cannot find auth client by id.", e);
        }
    }

    @Override
    public void persist(OAuthClientVO oAuthClient) {
        OAuthClientVO createdVO = merge(oAuthClient);
        oAuthClient.setId(createdVO.getId());
    }

    @Override
    public OAuthClientVO merge(OAuthClientVO oAuthClient) {
        try {
            if (oAuthClient.getId() == null) {
                oAuthClient.setId(getId(COUNTERS_LOCATION));
            }
            Location location = new Location(OAUTH_CLIENT_NS, String.valueOf(oAuthClient.getId()));
            StoreValue storeOp = new StoreValue.Builder(RiakOAuthClient.convert(oAuthClient))
                    .withLocation(location)
                    .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                    .build();
            client.execute(storeOp);
            return oAuthClient;
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot store OAuthClient.", e);
        }
    }

    @Override
    public List<OAuthClientVO> list(String name,
                                 String namePattern,
                                 String domain,
                                 String oauthId,
                                 String sortField,
                                 Boolean sortOrderAsc,
                                 Integer take,
                                 Integer skip) {
        try {
            String sortFunction = sortMap.get(sortField);
            if (sortFunction == null) {
                sortFunction = sortMap.get("id");
            }
            if (sortOrderAsc == null) {
                sortOrderAsc = true;
            }
            BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                    .withNamespace(OAUTH_CLIENT_NS)
                    .withMapPhase(Function.newAnonymousJsFunction("function(riakObject, keyData, arg) { " +
                            "                if(riakObject.values[0].metadata['X-Riak-Deleted']){ return []; } " +
                            "                else { return Riak.mapValuesJson(riakObject, keyData, arg); }}"))
                    .withReducePhase(Function.newAnonymousJsFunction("function(values, arg) {" +
                            "return values.filter(function(v) {" +
                            "if (v === []) { return false; }" +
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
            }

            if (namePattern != null) {
                namePattern = namePattern.replace("%", "");
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "if (name === null) { return false; }" +
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
                                "return domain == '%s';" +
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
                                "return oauthId == '%s';" +
                                "})" +
                                "}", oauthId);
                Function reduceFunction = Function.newAnonymousJsFunction(functionString);
                builder.withReducePhase(reduceFunction);
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
            List<RiakOAuthClient> result = response.getResultsFromAllPhases(RiakOAuthClient.class).stream()
                    .skip(skip)
                    .limit(take)
                    .collect(Collectors.toList());

            Stream<OAuthClientVO> objectStream = result.stream().map(RiakOAuthClient::convert);
            return objectStream.collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot fetch OAuthClient by filter.", e);
        }
    }
}
