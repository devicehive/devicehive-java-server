package com.devicehive.dao.riak;

import com.basho.riak.client.api.commands.indexes.BinIndexQuery;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.api.commands.mapreduce.BucketMapReduce;
import com.basho.riak.client.api.commands.mapreduce.MapReduce;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.dao.OAuthClientDao;
import com.devicehive.dao.riak.model.RiakOAuthClient;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.vo.OAuthClientVO;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Profile({"riak"})
@Repository
public class OAuthClientDaoRiakImpl extends RiakGenericDao implements OAuthClientDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthClientDaoRiakImpl.class);

    private static final Namespace OAUTH_CLIENT_NS = new Namespace("oauth_client");

    private static final Location COUNTERS_LOCATION = new Location(new Namespace("counters", "dh_counters"),
            "oauthClientCounter");

    public OAuthClientDaoRiakImpl() {
    }

    @Override
    public int deleteById(Long id) {
        Location location = new Location(OAUTH_CLIENT_NS, String.valueOf(id));
        DeleteValue deleteOp = new DeleteValue.Builder(location).build();
        try {
            client.execute(deleteOp);
            return 1;
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error("Exception accessing Riak Storage.", e);
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
            LOGGER.error("Exception accessing Riak Storage.", e);
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
            LOGGER.error("Exception accessing Riak Storage.", e);
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
            LOGGER.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot store OAuthClient.", e);
        }
    }

    @Override
    public List<OAuthClientVO> get(String name,
            String namePattern,
            String domain,
            String oauthId,
            String sortField,
            Boolean isSortOrderAsc,
            Integer take,
            Integer skip) {
        try {
            BucketMapReduce.Builder builder = new BucketMapReduce.Builder()
                    .withNamespace(OAUTH_CLIENT_NS);
            addMapValues(builder);
            if (name != null) {
                addReduceFilter(builder, "name", FilterOperator.EQUAL, namePattern);
            } else if (namePattern != null) {
                namePattern = namePattern.replace("%", "");
                addReduceFilter(builder, "name", FilterOperator.REGEX, namePattern);
            }
            addReduceFilter(builder, "domain", FilterOperator.EQUAL, domain);
            addReduceFilter(builder, "oauthId", FilterOperator.EQUAL, oauthId);
            addReduceSort(builder, sortField, isSortOrderAsc);
            addReducePaging(builder, true, take, skip);

            MapReduce.Response response = client.execute(builder.build());
            Collection<RiakOAuthClient> result = response.getResultsFromAllPhases(RiakOAuthClient.class);
            return result.stream().map(RiakOAuthClient::convert).collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot fetch OAuthClient by filter.", e);
        }
    }
}
