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
    || [ -z "$HC_GROUP_PASSWORD" ]
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
    exit 1
fi

## By default Auth uses 'ws-kafka-proxy' Spring profile. In case 'rpc-client' profile is used
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

if [ "$SPRING_PROFILES_ACTIVE" = "rpc-client" ]
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
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dbootstrap.servers=${DH_KAFKA_BOOTSTRAP_SERVERS}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dzookeeper.connect=${DH_ZK_ADDRESS}:${DH_ZK_PORT}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dzookeeper.connectionTimeout=${DH_ZK_CONNECTIONTIMEOUT:-8000}"
    RPC_PARAMETERS="${RPC_PARAMETERS} -Dzookeeper.sessionTimeout=${DH_ZK_SESSIONTIMEOUT:-10000}"
fi

echo "Starting DeviceHive auth"
java -server -Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:+ExitOnOutOfMemoryError -jar \
-Dcom.devicehive.log.level="${DH_LOG_LEVEL:-INFO}" \
-Dhazelcast.cluster.members="${HC_MEMBERS}:${HC_PORT}" \
-Dhazelcast.group.name="${HC_GROUP_NAME}" \
-Dhazelcast.group.password="${HC_GROUP_PASSWORD}" \
-Droot.log.level="${ROOT_LOG_LEVEL:-WARN}" \
-Dserver.context-path=/auth \
-Dserver.port=8090 \
-Dspring.datasource.url="jdbc:postgresql://${DH_POSTGRES_ADDRESS}:${DH_POSTGRES_PORT}/${DH_POSTGRES_DB}" \
-Dspring.datasource.username="${DH_POSTGRES_USERNAME}" \
-Dspring.datasource.password="${DH_POSTGRES_PASSWORD}" \
-Dproxy.connect="${DH_WS_PROXY:-localhost:3000}" \
${RPC_PARAMETERS} \
"./devicehive-auth-${DH_VERSION}-boot.jar" &
PID=$!
wait "$PID"
