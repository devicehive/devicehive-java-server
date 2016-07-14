package com.devicehive.dao.riak;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.devicehive.dao.IdentityProviderDao;
import com.devicehive.model.IdentityProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.concurrent.ExecutionException;

@Profile({"riak"})
@Repository
public class IdentityProviderDaoImpl implements IdentityProviderDao {

    private static final Namespace CONFIG_NS = new Namespace("identity_provider");

    @Autowired
    private RiakClient client;


    @Override
    public IdentityProvider getByName(@NotNull String name) {
        //TODO configurable quorum?
        try {
            Location objectKey = new Location(CONFIG_NS, name);
            FetchValue fetch = new FetchValue.Builder(objectKey).build();
            FetchValue.Response response = client.execute(fetch);
            if (response.isNotFound()) {
                return null;
            }
            IdentityProvider identityProvider = response.getValue(IdentityProvider.class);
            return identityProvider;
        } catch (ExecutionException | InterruptedException e) {
            //TODO throw exception here
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteById(@NotNull String name) {
        try {
            Location objectKey = new Location(CONFIG_NS, name);
            DeleteValue delete = new DeleteValue.Builder(objectKey).build();
            client.execute(delete);
        } catch (ExecutionException | InterruptedException e) {
            //TODO throw exception here
            throw new RuntimeException("Not implemented", e);
        }
        return true;
    }

    @Override
    public IdentityProvider merge(IdentityProvider existing) {
        try {
            Location objectKey = new Location(CONFIG_NS, existing.getName());
            StoreValue storeOp = new StoreValue.Builder(existing).withLocation(objectKey).build();
            client.execute(storeOp);
        } catch (ExecutionException | InterruptedException e) {
            //TODO throw exception here
            throw new RuntimeException(e);
        }
        return existing;
    }

    @Override
    public void persist(IdentityProvider identityProvider) {
        merge(identityProvider);
    }
}
