#!/bin/bash -x
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
    -d "{\"id\": 1, \"login\":\"dhadmin\", \"passwordHash\":\"DFXFrZ8VQIkOYECScBbBwsYinj+o8IlaLsRQ81wO+l8=\", \"passwordSalt\":\"sjQbZgcCmFxqTV4CCmGwpIHO\", \"role\":\"ADMIN\", \"status\":\"ACTIVE\", \"loginAttempts\":0, \"lastLogin\":null,\"googleLogin\":null,\"facebookLogin\":null,\"githubLogin\":null,\"entityVersion\":0,\"data\":null}" \
    'http://127.0.0.1:8098/types/default/buckets/user/keys/1'

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-login_bin: test_admin' \
    -d "{\"id\": 2, \"login\":\"test_admin\", \"passwordHash\":\"+IC4w+NeByiymEWlI5H1xbtNe4YKmPlLRZ7j3xaireg=\", \"passwordSalt\":\"9KynX3ShWnFym4y8Dla039py\", \"role\":\"ADMIN\", \"status\":\"ACTIVE\", \"loginAttempts\":0, \"lastLogin\":null,\"googleLogin\":null,\"facebookLogin\":null,\"githubLogin\":null,\"entityVersion\":0,\"data\":null}" \
    'http://127.0.0.1:8098/types/default/buckets/user/keys/2'

curl -XPOST \
    -H "Content-Type: application/json" \
    -d '{"increment": 100}' \
    'http://127.0.0.1:8098/types/counters/buckets/user_counters/datatypes/user_counter'

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-label_bin: Access Key for dhadmin' \
    -H 'x-riak-index-userId_int: 2' \
    -H 'x-riak-index-key_bin: 1jwKgLYi/CdfBTI9KByfYxwyQ6HUIEfnGSgakdpFjgk=' \
    -H 'x-riak-index-expirationDate_int: -1' \
    -d "{\"id\": 1, \"label\":\"Access Key for dhadmin\", \"key\": \"1jwKgLYi/CdfBTI9KByfYxwyQ6HUIEfnGSgakdpFjgk=\", \"expirationDate\": null, \"type\":\"DEFAULT\", \"user\":{\"id\":2,\"login\":\"test_admin\",\"passwordHash\":\"+IC4w+NeByiymEWlI5H1xbtNe4YKmPlLRZ7j3xaireg=\",\"passwordSalt\":\"9KynX3ShWnFym4y8Dla039py\",\"loginAttempts\":0,\"role\":\"ADMIN\",\"status\":\"ACTIVE\",\"lastLogin\":null,\"googleLogin\":null,\"facebookLogin\":null,\"githubLogin\":null,\"entityVersion\":1,\"data\":null}, \"permissions\": [{\"id\":null,\"domains\":null,\"subnets\":null,\"actions\":null,\"networkIds\":null,\"deviceGuids\":null}]}" \
    'http://127.0.0.1:8098/types/default/buckets/accessKey/keys/1'

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-label_bin: Access Key for dhadmin' \
    -H 'x-riak-index-userId_int: 1' \
    -H 'x-riak-index-key_bin: 1jwKgLYi/CdfBTI9KByfYxwyQ6HUIEfnGSgakdpFjgk=' \
    -H 'x-riak-index-expirationDate_int: -1' \
    -d "{\"id\": 2, \"label\":\"Access Key for dhadmin\", \"key\": \"1jwKgLYi/CdfBTI9KByfYxwyQ6HUIEfnGSgakdpFjgk=\", \"expirationDate\": null, \"type\":\"DEFAULT\", \"user\":{\"id\":1,\"login\":\"dhadmin\",\"passwordHash\":\"DFXFrZ8VQIkOYECScBbBwsYinj+o8IlaLsRQ81wO+l8=\",\"passwordSalt\":\"sjQbZgcCmFxqTV4CCmGwpIHO\",\"loginAttempts\":0,\"role\":\"ADMIN\",\"status\":\"ACTIVE\",\"lastLogin\":null,\"googleLogin\":null,\"facebookLogin\":null,\"githubLogin\":null,\"entityVersion\":0,\"data\":null}, \"permissions\": [{\"id\":null,\"domains\":null,\"subnets\":null,\"actions\":null,\"networkIds\":null,\"deviceGuids\":null}]}" \
    'http://127.0.0.1:8098/types/default/buckets/accessKey/keys/2'

curl -XPOST \
    -H "Content-Type: application/json" \
    -d '{"increment": 100}' \
    'http://127.0.0.1:8098/types/counters/buckets/check_counters/datatypes/accessKeyCounter'

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-name_bin: Sample VirtualLed Device' \
    -d "{\"id\":1, \"name\":\"Sample VirtualLed Device\", \"permanent\": false, \"offlineTimeout\": 600, \"data\":null,\"equipment\":[]}" \
    'http://127.0.0.1:8098/types/default/buckets/deviceClass/keys/1'

curl -XPOST \
    -H "Content-Type: application/json" \
    -d '{"increment": 100}' \
    'http://127.0.0.1:8098/types/counters/buckets/check_counters/datatypes/deviceClassCounter'

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-name_bin: VirtualLed Sample Network' \
    -d "{\"id\":1,\"key\":null,\"name\":\"VirtualLed Sample Network\", \"description\":\"A DeviceHive network for VirtualLed sample\",\"entityVersion\":null}" \
    'http://127.0.0.1:8098/types/default/buckets/network/keys/1'

curl -XPOST \
    -H "Content-Type: application/json" \
    -d '{"increment": 100}' \
    'http://127.0.0.1:8098/types/counters/buckets/network_counters/datatypes/network_counter'

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-guid_bin: E50D6085-2ABA-48E9-B1C3-73C673E414BE' \
    -d "{\"id\":1, \"guid\":\"E50D6085-2ABA-48E9-B1C3-73C673E414BE\", \"name\":\"Sample VirtualLed Device\", \"status\":\"Offline\", \"network\": {\"id\":1,\"key\":null,\"name\":\"VirtualLed Sample Network\", \"description\":\"A DeviceHive network for VirtualLed sample\",\"entityVersion\":null}, \"deviceClass\":{\"id\":1, \"name\":\"Sample VirtualLed Device\", \"permanent\": false, \"offlineTimeout\": 600, \"data\":null,\"equipment\":null}, \"blocked\":null}" \
    'http://127.0.0.1:8098/types/default/buckets/device/keys/1'

curl -XPOST \
    -H "Content-Type: application/json" \
    -d '{"increment": 100}' \
    'http://127.0.0.1:8098/types/counters/buckets/device_counters/datatypes/device_counter'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":\"true\"}" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/google.identity.allowed'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":\"google_id\"" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/google.identity.client.id'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":\"true\"}" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/facebook.identity.allowed'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":\"facebook_id\"}" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/facebook.identity.client.id'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":\"true\"}" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/github.identity.allowed'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":\"github_id\"}" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/github.identity.client.id'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":\"true\"}" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/allowNetworkAutoCreate'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":\"http://127.0.0.1:8080/cassandra\"}" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/cassandra.rest.endpoint'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":120000}" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/websocket.ping.timeout'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":1200000}" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/session.timeout'

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":1000}" \
    'http://127.0.0.1:8098/types/default/buckets/configuration/keys/user.login.lastTimeout'

curl http://127.0.0.1:8098/buckets/user/keys/1
