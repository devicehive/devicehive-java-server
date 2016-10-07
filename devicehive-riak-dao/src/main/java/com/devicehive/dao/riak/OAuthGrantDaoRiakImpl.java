package com.devicehive.dao.riak;

import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.api.commands.mapreduce.MapReduce;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
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
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Profile({"riak"})
@Repository
public class OAuthGrantDaoRiakImpl extends RiakGenericDao implements OAuthGrantDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthClientDaoRiakImpl.class);

    private static final Namespace OAUTH_GRANT_NS = new Namespace("oauth_grant");

    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "dh_counters"),
            "oauthGrantCounter");

    @Autowired
    private OAuthClientDao oAuthClientDao;

    @Autowired
    private AccessKeyDao accessKeyDao;

    @Autowired
    private UserDao userDao;

    public OAuthGrantDaoRiakImpl() {
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
            LOGGER.error("Exception accessing Riak Storage.", e);
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
            LOGGER.error("Exception accessing Riak Storage.", e);
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
                    OAuthClientVO clienVO = oAuthClientDao.find(oAuthGrant.getClient().getId());
                    if (clienVO.getOauthId().equals(clientOAuthID)) {
                        return restoreRefs(oAuthGrant, null);
                    }
                }
            }
            return null;
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error("Exception accessing Riak Storage.", e);
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
            LOGGER.error("Exception accessing Riak Storage.", e);
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
            Boolean isSortOrderAsc,
            Integer take,
            Integer skip) {

        BucketMapReduce.Builder builder = new BucketMapReduce.Builder();

        addMapValues(builder);
        addReduceFilter(builder, "userId", FilterOperator.EQUAL, user.getId());
        addReduceFilter(builder, "timestamp", FilterOperator.MORE, start);
        addReduceFilter(builder, "timestamp", FilterOperator.LESS, end);
        addReduceFilter(builder, "client.oauthId", FilterOperator.EQUAL, clientOAuthId);
        if (type != null) {
            String typeString = Type.getValueForIndex(type).toString();
            addReduceFilter(builder, "type", FilterOperator.EQUAL, typeString);
        }
        addReduceFilter(builder, "scope", FilterOperator.EQUAL, scope);
        addReduceFilter(builder, "redirectUri", FilterOperator.EQUAL, redirectUri);
        if (accessType != null) {
            String accessTypeString = AccessType.getValueForIndex(accessType).toString();
            addReduceFilter(builder, "accessType", FilterOperator.EQUAL, accessTypeString);
        }
        addReduceSort(builder, sortField, isSortOrderAsc);
        addReducePaging(builder, true, take, skip);
        try {
            MapReduce.Response response = client.execute(builder.build());
            Collection<RiakOAuthGrant> grants = response.getResultsFromAllPhases(RiakOAuthGrant.class);
            return grants.stream().map(RiakOAuthGrant::convert).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Exception accessing Riak Storage.", e);
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
