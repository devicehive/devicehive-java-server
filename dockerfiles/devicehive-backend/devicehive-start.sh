#!/bin/bash -e

set -x

trap 'terminate' TERM INT

terminate() {
    echo "SIGTERM received, terminating $PID"
    kill -TERM "$PID"
    wait "$PID"
}

# Check if all required parameters are set
if [ -z "$DH_POSTGRES_ADDRESS" ] \
    || [ -z "$DH_POSTGRES_USERNAME" ] \
    || [ -z "$DH_POSTGRES_PASSWORD" ] \
    || [ -z "$DH_POSTGRES_DB" ] \
    || [ -z "$HC_MEMBERS" ] \
    || [ -z "$HC_GROUP_NAME" ] \
    || [ -z "$HC_GROUP_PASSWORD" ] \
    || [ -z "$DH_ZK_ADDRESS" ] \
    || ( [ -z "$DH_KAFKA_BOOTSTRAP_SERVERS" ] && [ -z "$DH_KAFKA_ADDRESS" ] )
then
    echo "Some of required environment variables are not set or empty."
    echo "Please check following vars are passed to container:"
    echo "- DH_POSTGRES_ADDRESS"
    echo "- DH_POSTGRES_USERNAME"
    echo "- DH_POSTGRES_PASSWORD"
    echo "- DH_POSTGRES_DB"
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

# Check if Zookeper, Kafka, Postgres and Hazelcast are ready
while true; do
    nc -v -z -w1 "$DH_ZK_ADDRESS" "${DH_ZK_PORT:=2181}"
    result_zk=$?
    FIRST_KAFKA_SERVER="${DH_KAFKA_BOOTSTRAP_SERVERS%%,*}"
    nc -v -z -w1 "${FIRST_KAFKA_SERVER%%:*}" $(expr $FIRST_KAFKA_SERVER : '.*:\([0-9]*\)')
    result_kafka=$?
    nc -v -z -w1 "$DH_POSTGRES_ADDRESS" "${DH_POSTGRES_PORT:=5432}"
    result_postgres=$?
    nc -v -z -w1 "${HC_MEMBERS%%,*}" "${HC_PORT:=5701}"
    result_hc=$?

    if [ "$result_kafka" -eq 0 ] && [ "$result_postgres" -eq 0 ] && [ "$result_zk" -eq 0 ] && [ "$result_hc" -eq 0 ]; then
        break
    fi
    sleep 3
done

echo "Starting DeviceHive backend"
java -server -Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:+ExitOnOutOfMemoryError -jar \
-Dacks="${DH_ACKS:-1}" \
-Dauto.commit.interval.ms="${DH_AUTO_COMMIT_INTERVAL_MS:-5000}" \
-Dbatch.size="${DH_BATCH_SIZE:-98304}" \
-Dbootstrap.servers="${DH_KAFKA_BOOTSTRAP_SERVERS}" \
-Dcom.devicehive.log.level="${DH_LOG_LEVEL:-WARN}" \
-Denable.auto.commit="${DH_ENABLE_AUTO_COMMIT:-true}" \
-Dfetch.max.wait.ms="${DH_FETCH_MAX_WAIT_MS:-100}" \
-Dfetch.min.bytes="${DH_FETCH_MIN_BYTES:-1}" \
-Dhazelcast.cluster.members="${HC_MEMBERS}:${HC_PORT}" \
-Dhazelcast.group.name="${HC_GROUP_NAME}" \
-Dhazelcast.group.password="${HC_GROUP_PASSWORD}" \
-Dproxy.connect="${DH_WS_PROXY:-localhost:3000}" \
-Dproxy.worker.threads="${DH_WS_PROXY_BE_THREADS:-3}" \
-Dreplication.factor="${DH_REPLICATION_FACTOR:-1}" \
-Droot.log.level="${ROOT_LOG_LEVEL:-WARN}" \
-Drpc.server.request-consumer.threads="${DH_RPC_SERVER_REQ_CONS_THREADS:-3}" \
-Drpc.server.worker.threads="${DH_RPC_SERVER_WORKER_THREADS:-3}" \
-Dspring.datasource.url="jdbc:postgresql://${DH_POSTGRES_ADDRESS}:${DH_POSTGRES_PORT}/${DH_POSTGRES_DB}" \
-Dspring.datasource.username="${DH_POSTGRES_USERNAME}" \
-Dspring.datasource.password="${DH_POSTGRES_PASSWORD}" \
-Dzookeeper.connect="${DH_ZK_ADDRESS}:${DH_ZK_PORT}" \
-Dzookeeper.connectionTimeout="${DH_ZK_CONNECTIONTIMEOUT:-8000}" \
-Dzookeeper.sessionTimeout="${DH_ZK_SESSIONTIMEOUT:-10000}" \
"./devicehive-backend-${DH_VERSION}-boot.jar" &
PID=$!
wait $PID
