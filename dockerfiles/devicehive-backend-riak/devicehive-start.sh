#!/bin/bash -e

set -x

trap 'terminate' TERM INT

terminate() {
    echo "SIGTERM received, terminating $PID"
    kill -TERM "$PID"
    wait "$PID"
}

# Check if all required parameters are set
if [ -z "$DH_RIAK_HOST" ] \
    || [ -z "$DH_RIAK_PORT" ] \
    || [ -z "$DH_RIAK_HOST_MEMBER" ] \
    || [ -z "$DH_RIAK_HTTP_PORT" ] \
    || [ -z "$HC_MEMBERS" ] \
    || [ -z "$HC_GROUP_NAME" ] \
    || [ -z "$HC_GROUP_PASSWORD" ] \
    || [ -z "$DH_ZK_ADDRESS" ] \
    || ( [ -z "$DH_KAFKA_BOOTSTRAP_SERVERS" ] && [ -z "$DH_KAFKA_ADDRESS" ] )
then
    echo "Some of required environment variables are not set or empty."
    echo "Please check following vars are passed to container:"
    echo "- DH_RIAK_HOST"
    echo "- DH_RIAK_PORT"
    echo "- DH_RIAK_HOST_MEMBER"
    echo "- DH_RIAK_HTTP_PORT"
    echo "- HC_MEMBERS"
    echo "- HC_GROUP_NAME"
    echo "- HC_GROUP_PASSWORD"
    echo "- DH_ZK_ADDRESS"
    echo "And one of variants of Kafka bootstrap parameters:"
    echo "- DH_KAFKA_BOOTSTRAP_SERVERS for multiple servers"
    echo "or"
    echo "- DH_KAFKA_ADDRESS for a single server"
    exit 1
fi

if [ -z "$DH_KAFKA_BOOTSTRAP_SERVERS" ]
then
    DH_KAFKA_BOOTSTRAP_SERVERS="${DH_KAFKA_ADDRESS}:${DH_KAFKA_PORT:-9092}"
fi

# Check if Zookeper, Kafka and riak are ready
while true; do
    nc -v -z -w1 "$DH_ZK_ADDRESS" "${DH_ZK_PORT:=2181}"
    result_zk=$?
    FIRST_KAFKA_SERVER="${DH_KAFKA_BOOTSTRAP_SERVERS%%,*}"
    nc -v -z -w1 "${FIRST_KAFKA_SERVER%%:*}" $(expr $FIRST_KAFKA_SERVER : '.*:\([0-9]*\)')
    result_kafka=$?
    curl --output /dev/null --silent --head --fail "http://${DH_RIAK_HOST_MEMBER}:${DH_RIAK_HTTP_PORT}/ping"
    result_riak=$?
    nc -v -z -w1 "${HC_MEMBERS%%,*}" "${HC_PORT:=5701}"
    result_hc=$?

    if [ "$result_kafka" -eq 0 ] && [ "$result_zk" -eq 0 ] && [ "$result_riak" -eq 0 ] && [ "$result_hc" -eq 0 ]; then
        break
    fi
    sleep 5
done

echo "Setting Riak"
curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-login_bin: dhadmin' \
    -d "{\"id\": 1, \"login\":\"dhadmin\", \"passwordHash\":\"DFXFrZ8VQIkOYECScBbBwsYinj+o8IlaLsRQ81wO+l8=\", \"passwordSalt\":\"sjQbZgcCmFxqTV4CCmGwpIHO\", \"role\":\"ADMIN\", \"status\":\"ACTIVE\", \"loginAttempts\":0, \"lastLogin\":null,\"entityVersion\":0,\"data\":null}" \
    "http://${DH_RIAK_HOST}:${DH_RIAK_HTTP_PORT}/types/default/buckets/user/keys/1"

curl -XPOST \
    -H "Content-Type: application/json" \
    -d '{"increment": 100}' \
    "http://${DH_RIAK_HOST}:${DH_RIAK_HTTP_PORT}/types/counters/buckets/dh_counters/datatypes/userCounter"


curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-label_bin: Access Key for dhadmin' \
    -H 'x-riak-index-userId_int: 1' \
    -H 'x-riak-index-key_bin: 1jwKgLYi/CdfBTI9KByfYxwyQ6HUIEfnGSgakdpFjgk=' \
    -H 'x-riak-index-expirationDate_int: -1' \
    -d "{\"id\": 1, \"label\":\"Access Key for dhadmin\", \"key\": \"1jwKgLYi/CdfBTI9KByfYxwyQ6HUIEfnGSgakdpFjgk=\", \"expirationDate\": null, \"type\":\"DEFAULT\", \"user\":{\"id\":1,\"login\":\"dhadmin\",\"passwordHash\":\"DFXFrZ8VQIkOYECScBbBwsYinj+o8IlaLsRQ81wO+l8=\",\"passwordSalt\":\"sjQbZgcCmFxqTV4CCmGwpIHO\",\"loginAttempts\":0,\"role\":\"ADMIN\",\"status\":\"ACTIVE\",\"lastLogin\":null,\"entityVersion\":0,\"data\":null}, \"permissions\": [{\"id\":null,\"domains\":null,\"subnets\":null,\"actions\":null,\"networkIds\":null,\"deviceIds\":null}]}" \
    "http://${DH_RIAK_HOST}:${DH_RIAK_HTTP_PORT}/types/default/buckets/access_key/keys/1"

curl -XPOST \
    -H "Content-Type: application/json" \
    -d '{"increment": 100}' \
    "http://${DH_RIAK_HOST}:${DH_RIAK_HTTP_PORT}/types/counters/buckets/dh_counters/datatypes/accessKeyCounter"

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-name_bin: VirtualLed Sample Network' \
    -d "{\"id\":1,\"key\":null,\"name\":\"VirtualLed Sample Network\", \"description\":\"A DeviceHive network for VirtualLed sample\",\"entityVersion\":null}" \
    "http://${DH_RIAK_HOST}:${DH_RIAK_HTTP_PORT}/types/default/buckets/network/keys/1"

curl -XPOST \
    -H "Content-Type: application/json" \
    -d '{"increment": 100}' \
    "http://${DH_RIAK_HOST}:${DH_RIAK_HTTP_PORT}/types/counters/buckets/dh_counters/datatypes/networkCounter"

curl -XPUT \
    -H "Content-Type: application/json" \
    -H 'x-riak-index-device_id_bin: e50d6085-2aba-48e9-b1c3-73c673e414be' \
    -d "{\"id\":1, \"deviceId\":\"e50d6085-2aba-48e9-b1c3-73c673e414be\", \"name\":\"Sample VirtualLed Device\", \"status\":\"Offline\", \"network\": {\"id\":1,\"key\":null,\"name\":\"VirtualLed Sample Network\", \"description\":\"A DeviceHive network for VirtualLed sample\",\"entityVersion\":null}, \"blocked\":null}" \
    "http://${DH_RIAK_HOST}:${DH_RIAK_HTTP_PORT}/types/default/buckets/device/keys/1"

curl -XPOST \
    -H "Content-Type: application/json" \
    -d '{"increment": 100}' \
    "http://${DH_RIAK_HOST}:${DH_RIAK_HTTP_PORT}/types/counters/buckets/dh_counters/datatypes/deviceCounter"

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":\"true\"}" \
    "http://${DH_RIAK_HOST}:${DH_RIAK_HTTP_PORT}/types/default/buckets/configuration/keys/user.anonymous_creation"

curl -XPUT \
    -H "Content-Type: application/json" \
    -d "{\"value\":1000}" \
    "http://${DH_RIAK_HOST}:${DH_RIAK_HTTP_PORT}/types/default/buckets/configuration/keys/user.login.lastTimeout"

echo "Starting DeviceHive backend"
java -server -Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -jar \
-Dacks="${DH_ACKS:-1}" \
-Dauto.commit.interval.ms="${DH_AUTO_COMMIT_INTERVAL_MS:-5000}" \
-Dbatch.size="${DH_BATCH_SIZE:-98304}" \
-Dbootstrap.servers="${DH_KAFKA_BOOTSTRAP_SERVERS}" \
-Dcom.devicehive.log.level="${DH_LOG_LEVEL:-WARN}" \
-Denable.auto.commit="${DH_ENABLE_AUTO_COMMIT:-true}" \
-Dfetch.max.wait.ms="${DH_FETCH_MAX_WAIT_MS:-100}" \
-Dfetch.min.bytes="${DH_FETCH_MIN_BYTES:-1}" \
-Dflyway.enabled=false \
-Dhazelcast.cluster.members="${HC_MEMBERS}:${HC_PORT}" \
-Dhazelcast.group.name="${HC_GROUP_NAME}" \
-Dhazelcast.group.password="${HC_GROUP_PASSWORD}" \
-Dreplication.factor="${DH_REPLICATION_FACTOR:-1}" \
-Droot.log.level="${ROOT_LOG_LEVEL:-WARN}" \
-Driak.host="${DH_RIAK_HOST}" \
-Driak.port="${DH_RIAK_PORT}" \
-Drpc.server.request-consumer.threads="${DH_RPC_SERVER_REQ_CONS_THREADS:-3}" \
-Drpc.server.worker.threads="${DH_RPC_SERVER_WORKER_THREADS:-3}" \
-Dzookeeper.connect="${DH_ZK_ADDRESS}:${DH_ZK_PORT}" \
-Dzookeeper.connectionTimeout="${DH_ZK_CONNECTIONTIMEOUT:-8000}" \
-Dzookeeper.sessionTimeout="${DH_ZK_SESSIONTIMEOUT:-10000}" \
"./devicehive-backend-${DH_VERSION}-boot.jar" &
PID=$!
wait  "$PID"
