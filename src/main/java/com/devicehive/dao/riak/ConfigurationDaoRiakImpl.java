package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.dao.ConfigurationDao;
import com.devicehive.exceptions.HivePersistenceLayerException;
import com.devicehive.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Profile({"riak"})
@Repository
public class ConfigurationDaoRiakImpl extends RiakGenericDao implements ConfigurationDao {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationDaoRiakImpl.class);

    private static final Namespace CONFIG_NS = new Namespace("configuration");

    @Autowired
    private RiakClient client;

    @Override
    public Optional<Configuration> getByName(String id) {
        //TODO configurable quorum?
        try {
            Location objectKey = new Location(CONFIG_NS, id);
            FetchValue fetch = new FetchValue.Builder(objectKey).build();
            FetchValue.Response response = client.execute(fetch);
            Configuration configuration = getOrNull(response, Configuration.class);
            return Optional.ofNullable(configuration);
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
    public void persist(Configuration configuration) {
        try {
            Location objectKey = new Location(CONFIG_NS, configuration.getName());
            StoreValue storeOp = new StoreValue.Builder(configuration).withLocation(objectKey).build();
            client.execute(storeOp);
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Exception accessing Riak Storage.", e);
            throw new HivePersistenceLayerException("Cannot store configuration.", e);
        }
    }

    @Override
    public Configuration merge(Configuration existing) {
        persist(existing);
        return existing;
    }
}
