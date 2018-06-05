#!/bin/bash -e

set -x

. /opt/devicehive/lib/helpers.sh

trap 'terminate' TERM INT

terminate() {
    echo "SIGTERM received, terminating $PID"
    kill -TERM "$PID"
    wait "$PID"
}

## Check if required parameters are set
if [ -z "$DH_POSTGRES_ADDRESS" ] \
    || [ -z "$DH_POSTGRES_USERNAME" ] \
    || [ -z "$DH_POSTGRES_PASSWORD" ] \
    || [ -z "$DH_POSTGRES_DB" ] \
    || [ -z "$HC_MEMBERS" ] \
    || [ -z "$HC_GROUP_NAME" ] \
    || [ -z "$HC_GROUP_PASSWORD" ] \
    || [ -z "$DH_AUTH_URL" ]
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
    echo "- DH_AUTH_URL"
    exit 1
fi

## By default Frontend uses 'ws-kafka-proxy-frontend' Spring profile. In case 'rpc-client' profile is used
## we need to check Kafka connection parameters
if [ "$SPRING_PROFILES_ACTIVE" = "rpc-client" ]
then
    if [ -z "$DH_ZK_ADDRESS" ] \
        || { [ -z "$DH_KAFKA_BOOTSTRAP_SERVERS" ] && [ -z "$DH_KAFKA_ADDRESS" ]; }
    then
        echo "Some of required environment variables are not set or empty."
        echo "Please check following vars are passed to container:"
        echo "- DH_ZK_ADDRESS"
        echo "And one of variants of Kafka bootstrap parameters:"
        echo "- DH_KAFKA_BOOTSTRAP_SERVERS for multiple servers"
        echo "or"
        echo "- DH_KAFKA_ADDRESS for a single server"
        exit 1
    fi

    ## At lease one of DH_KAFKA_BOOTSTRAP_SERVERS or DH_KAFKA_ADDRESS must be set now
    ## Construct if empty DH_KAFKA_BOOTSTRAP_SERVERS for later use
    if [ -z "$DH_KAFKA_BOOTSTRAP_SERVERS" ]
    then
        DH_KAFKA_BOOTSTRAP_SERVERS="${DH_KAFKA_ADDRESS}:${DH_KAFKA_PORT:-9092}"
    fi
fi

## Check if Postgres and Hazelcast are ready
while true; do
    test_port "$DH_POSTGRES_ADDRESS" "${DH_POSTGRES_PORT:=5432}" \
       && test_port "${HC_MEMBERS%%,*}" "${HC_PORT:=5701}" \
       && break
    sleep 3
done

if [ "$SPRING_PROFILES_ACTIVE" = "rpc-server" ]
then
    ## Check if Zookeper, Kafka, are ready
    while true; do
        FIRST_KAFKA_SERVER="${DH_KAFKA_BOOTSTRAP_SERVERS%%,*}"
        test_port "$DH_ZK_ADDRESS" "${DH_ZK_PORT:=2181}" \
          && test_port "${FIRST_KAFKA_SERVER%%:*}" "$(expr $FIRST_KAFKA_SERVER : '.*:\([0-9]*\)')" \
          && break
        sleep 3
    done
fi

RPC_PARAMETERS=""
if [ "$SPRING_PROFILES_ACTIVE" = "rpc-client" ]
then
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dacks=${DH_ACKS:-1}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dauto.commit.interval.ms=${DH_AUTO_COMMIT_INTERVAL_MS:-5000}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dbatch.size=${DH_BATCH_SIZE:-98304}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dbootstrap.servers=${DH_KAFKA_BOOTSTRAP_SERVERS}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Denable.auto.commit=${DH_ENABLE_AUTO_COMMIT:-true}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dfetch.max.wait.ms=${DH_FETCH_MAX_WAIT_MS:-100}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dfetch.min.bytes=${DH_FETCH_MIN_BYTES:-1}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dreplication.factor=${DH_REPLICATION_FACTOR:-1}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Drpc.client.request-consumer.threads=${DH_RPC_CLIENT_REQ_CONS_THREADS:-3}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dzookeeper.connect=${DH_ZK_ADDRESS}:${DH_ZK_PORT}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dzookeeper.connectionTimeout=${DH_ZK_CONNECTIONTIMEOUT:-8000}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dzookeeper.sessionTimeout=${DH_ZK_SESSIONTIMEOUT:-10000}"
fi

echo "Starting DeviceHive frontend"
java -server -Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:+ExitOnOutOfMemoryError -jar \
-Dcom.devicehive.log.level="${DH_LOG_LEVEL:-WARN}" \
-Dhazelcast.cluster.members="${HC_MEMBERS}:${HC_PORT}" \
-Dhazelcast.group.name="${HC_GROUP_NAME}" \
-Dhazelcast.group.password="${HC_GROUP_PASSWORD}" \
-Dproxy.connect="${DH_WS_PROXY:-localhost:3000}" \
-Dproxy.worker.threads="${DH_WS_PROXY_THREADS:-3}" \
-Droot.log.level="${ROOT_LOG_LEVEL:-WARN}" \
-Dserver.context-path=/api \
-Dserver.port=8080 \
-Dauth.base.url="${DH_AUTH_URL}" \
-Dspring.datasource.url="jdbc:postgresql://${DH_POSTGRES_ADDRESS}:${DH_POSTGRES_PORT}/${DH_POSTGRES_DB}" \
-Dspring.datasource.username="${DH_POSTGRES_USERNAME}" \
-Dspring.datasource.password="${DH_POSTGRES_PASSWORD}" \
${RPC_PARAMETERS} \
"./devicehive-frontend-${DH_VERSION}-boot.jar" &
PID=$!
wait "$PID"
