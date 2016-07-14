package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.dao.ConfigurationDao;
import com.devicehive.model.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Profile({"riak"})
@Repository
public class ConfigurationDaoImpl implements ConfigurationDao {

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
            if (response.isNotFound()) {
                return Optional.empty();
            }
            Configuration configurationValue = response.getValue(Configuration.class);
            return Optional.of(configurationValue);
        } catch (ExecutionException | InterruptedException e) {
            //TODO throw exception here
            throw new RuntimeException(e);
        }
    }

    @Override
    public int delete(String name) {
        try {
            Location objectKey = new Location(CONFIG_NS, name);
            DeleteValue delete = new DeleteValue.Builder(objectKey).build();
            client.execute(delete);
        } catch (ExecutionException | InterruptedException e) {
            //TODO throw exception here
            throw new RuntimeException("Not implemented", e);
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
            //TODO throw exception here
            throw new RuntimeException(e);
        }
    }

    @Override
    public Configuration merge(Configuration existing) {
        persist(existing);
        return existing;
    }
}
