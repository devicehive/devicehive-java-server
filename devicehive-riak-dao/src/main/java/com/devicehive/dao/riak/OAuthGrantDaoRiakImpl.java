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
import com.devicehive.configuration.Constants;
import com.devicehive.dao.AccessKeyDao;
import com.devicehive.dao.OAuthClientDao;
import com.devicehive.dao.OAuthGrantDao;
import com.devicehive.dao.UserDao;
import com.devicehive.dao.riak.model.RiakOAuthGrant;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.enums.AccessType;
import com.devicehive.model.enums.Type;
import com.devicehive.vo.AccessKeyVO;
import com.devicehive.vo.OAuthClientVO;
import com.devicehive.vo.OAuthGrantVO;
import com.devicehive.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Profile({"riak"})
@Repository
public class OAuthGrantDaoRiakImpl extends RiakGenericDao implements OAuthGrantDao {

    private static final Logger logger = LoggerFactory.getLogger(OAuthClientDaoRiakImpl.class);

    private static final Namespace OAUTH_GRANT_NS = new Namespace("oauth_grant");

    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "dh_counters"),
            "oauthGrantCounter");

    @Autowired
    private RiakClient client;

    @Autowired
    private OAuthClientDao oAuthClientDao;

    @Autowired
    private AccessKeyDao accessKeyDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private RiakQuorum quorum;


    private final Map<String, String> sortMap = new HashMap<>();

    public OAuthGrantDaoRiakImpl() {
        sortMap.put("id", "function(a,b){ return a.id %s b.id; }");
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
    public OAuthGrantVO getByIdAndUser(UserVO user, Long grantId) {
        RiakOAuthGrant grant = get(grantId);
        if (grant != null && user != null && grant.getUserId() == user.getId()) {
            return restoreRefs(grant, null);
        } else {
            return null;
        }
    }

    @Override
    public OAuthGrantVO getById(Long grantId) {
        RiakOAuthGrant grant = get(grantId);
        return restoreRefs(grant, null);
    }

    private RiakOAuthGrant get(Long grantId) {
        try {
            Location location = new Location(OAUTH_GRANT_NS, String.valueOf(grantId));
            FetchValue fetchOp = new FetchValue.Builder(location)
                    .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                    .build();
            FetchValue.Response execute = client.execute(fetchOp);
            return getOrNull(execute, RiakOAuthGrant.class);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot find OAuthGrant by id.", e);
        }
    }


    @Override
    public int deleteByUserAndId(UserVO user, Long grantId) {
        try {
            Location location = new Location(OAUTH_GRANT_NS, String.valueOf(grantId));

            FetchValue fetchOp = new FetchValue.Builder(location)
                    .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                    .build();
            RiakOAuthGrant riakOAuthGrantgrant = getOrNull(client.execute(fetchOp), RiakOAuthGrant.class);
            OAuthGrantVO grant = restoreRefs(riakOAuthGrantgrant, null);

            if (grant != null && grant.getUser() != null && grant.getUser().equals(user)) {
                DeleteValue deleteOp = new DeleteValue.Builder(location).build();
                client.execute(deleteOp);
                return 1;
            } else {
                return 0;
            }

        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot delete OAuthGrant by id and user.", e);
        }
    }

    @Override
    public OAuthGrantVO getByCodeAndOAuthID(String authCode, String clientOAuthID) {
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
                        .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                        .build();
                FetchValue.Response execute = client.execute(fetchOp);
                RiakOAuthGrant oAuthGrant = getOrNull(execute, RiakOAuthGrant.class);
                if (oAuthGrant != null && oAuthGrant.getClient() != null) {
                    OAuthClientVO client = oAuthClientDao.find(oAuthGrant.getClient().getId());
                    if (client.getOauthId().equals(clientOAuthID)) {
                        return restoreRefs(oAuthGrant, null);
                    }
                }
            }
            return null;
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot fetch OAuthGrant by code and client.", e);
        }
    }

    @Override
    public OAuthGrantVO find(Long id) {
        return restoreRefs(get(id), null);
    }

    @Override
    public void persist(OAuthGrantVO oAuthGrant) {
        OAuthGrantVO persisted = merge(oAuthGrant);
        oAuthGrant.setId(persisted.getId());
    }

    @Override
    public OAuthGrantVO merge(OAuthGrantVO oAuthGrant) {
        try {
            if (oAuthGrant.getId() == null) {
                oAuthGrant.setId(getId(COUNTERS_LOCATION));
            }
            AccessKeyVO accessKey = oAuthGrant.getAccessKey();
            Location location = new Location(OAUTH_GRANT_NS, String.valueOf(oAuthGrant.getId()));
            RiakOAuthGrant riakOAuthGrant = removeRefs(oAuthGrant);
            StoreValue storeOp = new StoreValue.Builder(riakOAuthGrant)
                    .withLocation(location)
                    .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                    .build();
            client.execute(storeOp);
            return restoreRefs(riakOAuthGrant, accessKey);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot store OAuthGrant.", e);
        }
    }

    @Override
    public List<OAuthGrantVO> list(@NotNull UserVO user,
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
        try {
            String sortFunction = sortMap.get(sortField);
            if (sortFunction == null) {
                sortFunction = sortMap.get("id");
            }
            if (sortOrder == null) {
                sortOrder = true;
            }
            BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                    .withNamespace(OAUTH_GRANT_NS)
                    .withMapPhase(Function.newAnonymousJsFunction("function(riakObject, keyData, arg) { " +
                            "                if(riakObject.values[0].metadata['X-Riak-Deleted']){ return []; } " +
                            "                else { return Riak.mapValuesJson(riakObject, keyData, arg); }}"))
                    .withReducePhase(Function.newAnonymousJsFunction("function(values, arg) {" +
                            "return values.filter(function(v) {" +
                            "if (v === []) { return false; }" +
                            "return true;" +
                            "})" +
                            "}"));

            if (user != null) {
                long userId = user.getId();
                String functionString = String.format(
                        "function(values, arg) {" +
                                "return values.filter(function(v) {" +
                                "var id = v.userId;" +
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
                                "if (v.timestamp === null) { return false; }" +
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
                                "if (v.timestamp === null) { return false; }" +
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
                                "if (v.client === null) { return false; }" +
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
                                "}", redirectUri);
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
                    true);

            if (take == null)
                take = Constants.DEFAULT_TAKE;
            if (skip == null)
                skip = 0;

            BucketMapReduce bmr = builder.build();
            RiakFuture<MapReduce.Response, BinaryValue> future = client.executeAsync(bmr);
            future.await();
            MapReduce.Response response = future.get();
            List<RiakOAuthGrant> grants = response.getResultsFromAllPhases(RiakOAuthGrant.class).stream()
                    .skip(skip)
                    .limit(take)
                    .collect(Collectors.toList());
            return grants.stream().map(RiakOAuthGrant::convert).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot fetch OAuthGrant by filter.", e);
        }
    }

    private RiakOAuthGrant removeRefs(OAuthGrantVO grant) {
        return RiakOAuthGrant.convert(grant);
    }

    private OAuthGrantVO restoreRefs(RiakOAuthGrant grant, AccessKeyVO accessKey) {
        OAuthGrantVO vo = RiakOAuthGrant.convert(grant);
        if (vo != null) {
            if (accessKey != null) {
                vo.setAccessKey(accessKey);
            } else {
                vo.setAccessKey(accessKeyDao.find(grant.getAccessKeyId()));
            }

            UserVO user = userDao.find(grant.getUserId());
            if (user != null) {
                vo.setUser(user);
            }
        }
        return vo;
    }
}
