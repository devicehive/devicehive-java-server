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
    || [ -z "$DH_ZK_ADDRESS" ] \
    || ( [ -z "$DH_KAFKA_BOOTSTRAP_SERVERS" ] && [ -z "$DH_KAFKA_ADDRESS" ] )
then
    echo "Some of required environment variables are not set or empty."
    echo "Please check following vars are passed to container:"
    echo "- DH_POSTGRES_ADDRESS"
    echo "- DH_POSTGRES_USERNAME"
    echo "- DH_POSTGRES_PASSWORD"
    echo "- DH_POSTGRES_DB"
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

echo "Starting DeviceHive frontend"
java -server -Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -jar \
-Dacks="${DH_ACKS:-1}" \
-Dauto.commit.interval.ms="${DH_AUTO_COMMIT_INTERVAL_MS:-5000}" \
-Dbatch.size="${DH_BATCH_SIZE:-98304}" \
-Dbootstrap.servers="${DH_KAFKA_BOOTSTRAP_SERVERS}" \
-Dcom.devicehive.log.level="${DH_LOG_LEVEL:-WARN}" \
-Denable.auto.commit="${DH_ENABLE_AUTO_COMMIT:-true}" \
-Dfetch.max.wait.ms="${DH_FETCH_MAX_WAIT_MS:-100}" \
-Dfetch.min.bytes="${DH_FETCH_MIN_BYTES:-1}" \
-Dreplication.factor="${DH_REPLICATION_FACTOR:-1}" \
-Droot.log.level="${ROOT_LOG_LEVEL:-WARN}" \
-Drpc.client.response-consumer.threads="${DH_RPC_CLIENT_RES_CONS_THREADS:-3}" \
-Dserver.context-path=/api \
-Dserver.port=8080 \
-Dspring.datasource.url="jdbc:postgresql://${DH_POSTGRES_ADDRESS}:${DH_POSTGRES_PORT:-5432}/${DH_POSTGRES_DB}" \
-Dspring.datasource.username="${DH_POSTGRES_USERNAME}" \
-Dspring.datasource.password="${DH_POSTGRES_PASSWORD}" \
-Dzookeeper.connect="${DH_ZK_ADDRESS}:${DH_ZK_PORT:-2181}" \
-Dzookeeper.connectionTimeout="${DH_ZK_CONNECTIONTIMEOUT:-8000}" \
-Dzookeeper.sessionTimeout="${DH_ZK_SESSIONTIMEOUT:-10000}" \
"./devicehive-frontend-${DH_VERSION}-boot.jar" &
PID=$!
wait "$PID"
