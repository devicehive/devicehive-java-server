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
    || [ -z "$REDIS_MASTER_HOST" ] \
    || [ -z "$REDIS_MASTER_PORT" ] \
    || [ -z "$REDIS_MASTER_PASSWORD" ]
then
    echo "Some of required environment variables are not set or empty."
    echo "Please check following vars are passed to container:"
    echo "- DH_POSTGRES_ADDRESS"
    echo "- DH_POSTGRES_USERNAME"
    echo "- DH_POSTGRES_PASSWORD"
    echo "- DH_POSTGRES_DB"
    echo "- REDIS_MASTER_HOST"
    echo "- REDIS_MASTER_PORT"
    echo "- REDIS_MASTER_PASSWORD"
    exit 1
fi

if [ "$SPRING_PROFILES_ACTIVE" = "rpc-client" ]
then
    if [ -z "$DH_KAFKA_BOOTSTRAP_SERVERS" ] && [ -z "$DH_KAFKA_ADDRESS" ]
    then
        echo "Some of required environment variables are not set or empty."
        echo "Please check following vars are passed to container:"
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

# Check Kafka and Postgres are ready
while true; do
    if [ "$SPRING_PROFILES_ACTIVE" = "rpc-client" ]
    then
        FIRST_KAFKA_SERVER="${DH_KAFKA_BOOTSTRAP_SERVERS%%,*}"
        nc -v -z -w1 "${FIRST_KAFKA_SERVER%%:*}" $(expr $FIRST_KAFKA_SERVER : '.*:\([0-9]*\)')
        result_kafka=$?
    else
        result_kafka=0
    fi
    nc -v -z -w1 "$DH_POSTGRES_ADDRESS" "${DH_POSTGRES_PORT:=5432}"
    result_postgres=$?

if [ "$result_kafka" -eq 0 ] && [ "$result_postgres" -eq 0 ]; then
        break
    fi
    sleep 3
done
KAFKA_PARAMETERS=""
if [ "$SPRING_PROFILES_ACTIVE" = "rpc-client" ]
then
    KAFKA_PARAMETERS="-Dbootstrap.servers=${DH_KAFKA_BOOTSTRAP_SERVERS}"
fi

echo "Starting DeviceHive auth"
java -server -Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:+ExitOnOutOfMemoryError -jar \
-Dcom.devicehive.log.level="${DH_LOG_LEVEL:-WARN}" \
-Droot.log.level="${ROOT_LOG_LEVEL:-WARN}" \
-Dserver.servlet.context-path=/auth \
-Dserver.port=8090 \
-Dspring.datasource.url="jdbc:postgresql://${DH_POSTGRES_ADDRESS}:${DH_POSTGRES_PORT:-5432}/${DH_POSTGRES_DB}" \
-Dspring.datasource.username="${DH_POSTGRES_USERNAME}" \
-Dspring.datasource.password="${DH_POSTGRES_PASSWORD}" \
-Dproxy.connect="${DH_WS_PROXY:-localhost:3000}" \
$KAFKA_PARAMETERS \
"./devicehive-auth-${DH_VERSION}-boot.jar" &
PID=$!
wait "$PID"
