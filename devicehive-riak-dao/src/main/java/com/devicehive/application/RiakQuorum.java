package com.devicehive.application;

import com.basho.riak.client.api.cap.Quorum;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;

import java.util.HashMap;
import java.util.Map;

public class RiakQuorum {


    private FetchValue.Option<Quorum> readQuorumOption;
    private Quorum readQuorum;

    private StoreValue.Option<Quorum> writeQuorumOption;
    private Quorum writeQuorum;

    public RiakQuorum(FetchValue.Option<Quorum> readQuorumOption, Quorum readQuorum, StoreValue.Option<Quorum> writeQuorumOption, Quorum writeQuorum) {
        this.readQuorumOption = readQuorumOption;
        this.readQuorum = readQuorum;
        this.writeQuorumOption = writeQuorumOption;
        this.writeQuorum = writeQuorum;
    }

    public FetchValue.Option<Quorum> getReadQuorumOption() {
        return readQuorumOption;
    }

    public Quorum getReadQuorum() {
        return readQuorum;
    }

    public StoreValue.Option<Quorum> getWriteQuorumOption() {
        return writeQuorumOption;
    }

    public Quorum getWriteQuorum() {
        return writeQuorum;
    }
}
