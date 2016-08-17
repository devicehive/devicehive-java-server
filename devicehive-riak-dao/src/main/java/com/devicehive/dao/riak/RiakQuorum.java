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
