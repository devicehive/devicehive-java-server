package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.application.RiakQuorum;
import com.devicehive.dao.ConfigurationDao;
import com.devicehive.dao.riak.model.RiakConfiguration;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.vo.ConfigurationVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class ConfigurationDaoRiakImpl extends RiakGenericDao implements ConfigurationDao {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationDaoRiakImpl.class);

    private static final Namespace CONFIG_NS = new Namespace("configuration");

    @Autowired
    private RiakClient client;

    @Autowired
    private RiakQuorum quorum;

    @Override
    public Optional<ConfigurationVO> getByName(String id) {
        //TODO configurable quorum?
        try {
            Location objectKey = new Location(CONFIG_NS, id);
            FetchValue fetch = new FetchValue.Builder(objectKey)
                    .withOption(quorum.getReadQuorumOption(), quorum.getReadQuorum())
                    .build();
            FetchValue.Response response = client.execute(fetch);
            RiakConfiguration configuration = getOrNull(response, RiakConfiguration.class);
            return Optional.ofNullable(RiakConfiguration.convert(configuration));
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot fetch configuration by name.", e);
        }
    }

    @Override
    public int delete(String name) {
        try {
            Location objectKey = new Location(CONFIG_NS, name);
            DeleteValue delete = new DeleteValue.Builder(objectKey).build();
            client.execute(delete);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot delete configuration by name.", e);
        }
        return 1;
    }

    @Override
    public void persist(ConfigurationVO configuration) {
        try {
            Location objectKey = new Location(CONFIG_NS, configuration.getName());
            StoreValue storeOp = new StoreValue.Builder(RiakConfiguration.convert(configuration))
                    .withLocation(objectKey)
                    .withOption(quorum.getWriteQuorumOption(), quorum.getWriteQuorum())
                    .build();
            client.execute(storeOp);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot store configuration.", e);
        }
    }

    @Override
    public ConfigurationVO merge(ConfigurationVO existing) {
        persist(existing);
        return existing;
    }
}
