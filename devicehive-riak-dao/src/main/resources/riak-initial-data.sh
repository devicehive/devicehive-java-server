#!/bin/bash -x

###
# #%L
# DeviceHive Dao Riak Implementation
# %%
# Copyright (C) 2016 DataArt
# %%
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# #L%
###
create_counters_script()
{
  echo "Creating counters datatype"
  sudo riak-admin bucket-type create counters '{"props":{"datatype":"counter"}}'
  echo "Activating counters"
  sudo riak-admin bucket-type activate counters
}

set -e

create_counters_script || true

sudo service riak stop
sleep 10
sudo service riak start
sleep 20

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-login_bin: dhadmin' \
    -d "{\"id\": 1, \"login\":\"dhadmin\", \"passwordHash\":\"DFXFrZ8VQIkOYECScBbBwsYinj+o8IlaLsRQ81wO+l8=\", \"passwordSalt\":\"sjQbZgcCmFxqTV4CCmGwpIHO\", \"role\":\"ADMIN\", \"status\":\"ACTIVE\", \"loginAttempts\":0, \"lastLogin\":null,\"entityVersion\":0,\"data\":null}" \
    'http://127.0.0.1:8098/types/default/buckets/user/keys/1'

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-login_bin: test_admin' \
    -d "{\"id\": 2, \"login\":\"test_admin\", \"passwordHash\":\"+IC4w+NeByiymEWlI5H1xbtNe4YKmPlLRZ7j3xaireg=\", \"passwordSalt\":\"9KynX3ShWnFym4y8Dla039py\", \"role\":\"ADMIN\", \"status\":\"ACTIVE\", \"loginAttempts\":0, \"lastLogin\":null,\"entityVersion\":0,\"data\":null}" \
    'http://127.0.0.1:8098/types/default/buckets/user/keys/2'

curl -XPOST \
    -H "Content-Type: application/json" \
    -d '{"increment": 100}' \
    'http://127.0.0.1:8098/types/counters/buckets/dh_counters/datatypes/userCounter'

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-label_bin: Access Key for dhadmin' \
    -H 'x-riak-index-userId_int: 2' \
    -H 'x-riak-index-key_bin: 1jwKgLYi/CdfBTI9KByfYxwyQ6HUIEfnGSgakdpFjgk=' \
    -H 'x-riak-index-expirationDate_int: -1' \
    -d "{\"id\": 1, \"label\":\"Access Key for dhadmin\", \"key\": \"1jwKgLYi/CdfBTI9KByfYxwyQ6HUIEfnGSgakdpFjgk=\", \"expirationDate\": null, \"type\":\"DEFAULT\", \"user\":{\"id\":2,\"login\":\"test_admin\",\"passwordHash\":\"+IC4w+NeByiymEWlI5H1xbtNe4YKmPlLRZ7j3xaireg=\",\"passwordSalt\":\"9KynX3ShWnFym4y8Dla039py\",\"loginAttempts\":0,\"role\":\"ADMIN\",\"status\":\"ACTIVE\",\"lastLogin\":null,\"entityVersion\":1,\"data\":null}, \"permissions\": [{\"id\":null,\"domains\":null,\"subnets\":null,\"actions\":null,\"networkIds\":null,\"deviceGuids\":null}]}" \
    'http://127.0.0.1:8098/types/default/buckets/access_key/keys/1'

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-label_bin: Access Key for dhadmin' \
    -H 'x-riak-index-userId_int: 1' \
    -H 'x-riak-index-key_bin: 1jwKgLYi/CdfBTI9KByfYxwyQ6HUIEfnGSgakdpFjgk=' \
    -H 'x-riak-index-expirationDate_int: -1' \
    -d "{\"id\": 2, \"label\":\"Access Key for dhadmin\", \"key\": \"1jwKgLYi/CdfBTI9KByfYxwyQ6HUIEfnGSgakdpFjgk=\", \"expirationDate\": null, \"type\":\"DEFAULT\", \"user\":{\"id\":1,\"login\":\"dhadmin\",\"passwordHash\":\"DFXFrZ8VQIkOYECScBbBwsYinj+o8IlaLsRQ81wO+l8=\",\"passwordSalt\":\"sjQbZgcCmFxqTV4CCmGwpIHO\",\"loginAttempts\":0,\"role\":\"ADMIN\",\"status\":\"ACTIVE\",\"lastLogin\":null,\"entityVersion\":0,\"data\":null}, \"permissions\": [{\"id\":null,\"domains\":null,\"subnets\":null,\"actions\":null,\"networkIds\":null,\"deviceGuids\":null}]}" \
    'http://127.0.0.1:8098/types/default/buckets/access_key/keys/2'

curl -XPOST \
    -H "Content-Type: application/json" \
    -d '{"increment": 100}' \
    'http://127.0.0.1:8098/types/counters/buckets/dh_counters/datatypes/accessKeyCounter'

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-name_bin: VirtualLed Sample Network' \
    -d "{\"id\":1,\"key\":null,\"name\":\"VirtualLed Sample Network\", \"description\":\"A DeviceHive network for VirtualLed sample\",\"entityVersion\":null}" \
    'http://127.0.0.1:8098/types/default/buckets/network/keys/1'

curl -XPOST \
    -H "Content-Type: application/json" \
    -d '{"increment": 100}' \
    'http://127.0.0.1:8098/types/counters/buckets/dh_counters/datatypes/networkCounter'

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-guid_bin: E50D6085-2ABA-48E9-B1C3-73C673E414BE' \
    -d "{\"id\":1, \"guid\":\"E50D6085-2ABA-48E9-B1C3-73C673E414BE\", \"name\":\"Sample VirtualLed Device\", \"status\":\"Offline\", \"network\": {\"id\":1,\"key\":null,\"name\":\"VirtualLed Sample Network\", \"description\":\"A DeviceHive network for VirtualLed sample\",\"entityVersion\":null}, \"blocked\":null}" \
    'http://127.0.0.1:8098/types/default/buckets/device/keys/1'

curl -XPOST \
    -H "Content-Type: application/json" \
    -d '{"increment": 100}' \
    'http://127.0.0.1:8098/types/counters/buckets/dh_counters/datatypes/deviceCounter'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":\"true\"}" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/allowNetworkAutoCreate'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":\"true\"}" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/user.anonymous_creation'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":1000}" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/user.login.lastTimeout'

curl http://127.0.0.1:8098/buckets/user/keys/1
