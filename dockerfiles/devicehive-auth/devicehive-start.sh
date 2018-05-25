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

if [ "$SPRING_PROFILES_ACTIVE" = "rpc-client" ]
then
    if [ -z "$DH_ZK_ADDRESS" ] \
    || ( [ -z "$DH_KAFKA_BOOTSTRAP_SERVERS" ] && [ -z "$DH_KAFKA_ADDRESS" ] )
    then
        echo "Some of required environment variables are not set or empty."
        echo "Please check following vars are passed to container:"
        echo "- DH_ZK_ADDRESS"
        echo "And one of variants of Kafka bootstrap parameters:"
        echo "- DH_KAFKA_BOOTSTRAP_SERVERS for multiple servers"
        echo "or"
        echo "- DH_KAFKA_ADDRESS for a single server"
        exit 1
    else
      if [ -z "$DH_KAFKA_BOOTSTRAP_SERVERS" ]
      then
          DH_KAFKA_BOOTSTRAP_SERVERS="${DH_KAFKA_ADDRESS}:${DH_KAFKA_PORT:-9092}"
      fi
    fi
fi

# Check if Zookeper, Kafka, Postgres and Hazelcast are ready
while true; do
    if [ "$SPRING_PROFILES_ACTIVE" = "rpc-client" ]
    then
        nc -v -z -w1 "$DH_ZK_ADDRESS" "${DH_ZK_PORT:=2181}"
        result_zk=$?
        FIRST_KAFKA_SERVER="${DH_KAFKA_BOOTSTRAP_SERVERS%%,*}"
        nc -v -z -w1 "${FIRST_KAFKA_SERVER%%:*}" $(expr $FIRST_KAFKA_SERVER : '.*:\([0-9]*\)')
        result_kafka=$?
    else
        result_zk=0
        result_kafka=0
    fi
    nc -v -z -w1 "$DH_POSTGRES_ADDRESS" "${DH_POSTGRES_PORT:=5432}"
    result_postgres=$?
    nc -v -z -w1 "${HC_MEMBERS%%,*}" "${HC_PORT:=5701}"
    result_hc=$?

    if [ "$result_kafka" -eq 0 ] && [ "$result_postgres" -eq 0 ] && [ "$result_zk" -eq 0 ] && [ "$result_hc" -eq 0 ]; then
        break
    fi
    sleep 3
done
KAFKA_PARAMETERS=""
if [ "$SPRING_PROFILES_ACTIVE" = "rpc-client" ]
then
    KAFKA_PARAMETERS="-Dbootstrap.servers=${DH_KAFKA_BOOTSTRAP_SERVERS} -Dzookeeper.connect=${DH_ZK_ADDRESS}:${DH_ZK_PORT:-2181} -Dzookeeper.connectionTimeout=${DH_ZK_CONNECTIONTIMEOUT:-8000} -Dzookeeper.sessionTimeout=${DH_ZK_SESSIONTIMEOUT:-10000}"
fi

echo "Starting DeviceHive auth"
java -server -Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:+ExitOnOutOfMemoryError -jar \
-Dcom.devicehive.log.level="${DH_LOG_LEVEL:-WARN}" \
-Dhazelcast.cluster.members="${HC_MEMBERS}:${HC_PORT}" \
-Dhazelcast.group.name="${HC_GROUP_NAME}" \
-Dhazelcast.group.password="${HC_GROUP_PASSWORD}" \
-Droot.log.level="${ROOT_LOG_LEVEL:-WARN}" \
-Dserver.context-path=/auth \
-Dserver.port=8090 \
-Dspring.datasource.url="jdbc:postgresql://${DH_POSTGRES_ADDRESS}:${DH_POSTGRES_PORT:-5432}/${DH_POSTGRES_DB}" \
-Dspring.datasource.username="${DH_POSTGRES_USERNAME}" \
-Dspring.datasource.password="${DH_POSTGRES_PASSWORD}" \
-Dproxy.connect="${DH_WS_PROXY:-localhost:3000}" \
$KAFKA_PARAMETERS \
"./devicehive-auth-${DH_VERSION}-boot.jar" &
PID=$!
wait "$PID"
