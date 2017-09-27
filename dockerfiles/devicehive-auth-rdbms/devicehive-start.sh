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

# Check if Zookeper, Kafka, Postgres and Hazelcast are ready
while true; do
    nc -v -z -w1 "$DH_POSTGRES_ADDRESS" "${DH_POSTGRES_PORT:=5432}"
    result_postgres=$?
    nc -v -z -w1 "${HC_MEMBERS%%,*}" "${HC_PORT:=5701}"
    result_hc=$?

    if [ "$result_postgres" -eq 0 ] && [ "$result_hc" -eq 0 ]; then
        break
    fi
    sleep 3
done

echo "Starting DeviceHive auth"
java -server -Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -XX:+ExitOnOutOfMemoryError -jar \
-Dcom.devicehive.log.level="${DH_LOG_LEVEL:-WARN}" \
-Dhazelcast.cluster.members="${HC_MEMBERS}:${HC_PORT}" \
-Dhazelcast.group.name="${HC_GROUP_NAME}" \
-Dhazelcast.group.password="${HC_GROUP_PASSWORD}" \
-Droot.log.level="${ROOT_LOG_LEVEL:-WARN}" \
-Dserver.context-path=/api \
-Dserver.port=8090 \
-Dspring.datasource.url="jdbc:postgresql://${DH_POSTGRES_ADDRESS}:${DH_POSTGRES_PORT:-5432}/${DH_POSTGRES_DB}" \
-Dspring.datasource.username="${DH_POSTGRES_USERNAME}" \
-Dspring.datasource.password="${DH_POSTGRES_PASSWORD}" \
"./devicehive-auth-${DH_VERSION}-boot.jar" &
PID=$!
wait "$PID"
