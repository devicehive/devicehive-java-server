#!/bin/bash -e

set -x

trap 'terminate' TERM INT

terminate() {
   echo "SIGTERM received, terminating $PID"
   kill -TERM $PID
   wait $PID
}

# Check if all required parameters are set
if [ -z "$DH_ZK_ADDRESS" \
  -o -z "$DH_KAFKA_ADDRESS" \
  -o -z "$DH_POSTGRES_ADDRESS" \
  -o -z "$DH_POSTGRES_USERNAME" \
  -o -z "$DH_POSTGRES_PASSWORD" \
  -o -z "$DH_POSTGRES_DB" \
  -o -z "$HC_MEMBERS" \
  -o -z "$HC_GROUP_NAME" \
  -o -z "$HC_GROUP_PASSWORD" ]
then
    echo "Some of required environment variables are not set or empty."
    echo "Please check following vars are passed to container:"
    echo "- DH_ZK_ADDRESS"
    echo "- DH_KAFKA_ADDRESS"
    echo "- DH_POSTGRES_ADDRESS"
    echo "- DH_POSTGRES_USERNAME"
    echo "- DH_POSTGRES_PASSWORD"
    echo "- DH_POSTGRES_DB"
    echo "- HC_MEMBERS"
    echo "- HC_GROUP_NAME"
    echo "- HC_GROUP_PASSWORD"
    exit 1
fi
# Check if Zookeper, Kafka and Postgres are ready
while true; do
    nc -v -z -w1 $DH_ZK_ADDRESS ${DH_ZK_PORT:=2181}
    result_zk=$?
    nc -v -z -w1 $DH_KAFKA_ADDRESS ${DH_KAFKA_PORT:=9092}
    result_kafka=$?
    nc -v -z -w1 $DH_POSTGRES_ADDRESS ${DH_POSTGRES_PORT:=5432}
    result_postgres=$?
    nc -v -z -w1 ${HC_MEMBERS:%%,*} ${HC_PORT:=5701}
    result_hc=$?

    if [ "$result_kafka" -eq 0 -a "$result_postgres" -eq 0 -a "$result_zk" -eq 0 -a "$result_hc" -eq 0 ]; then
        break
    fi
    sleep 3
done

echo "Starting DeviceHive backend"
java -server -Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -jar \
-Dacks=${DH_ACKS:-1} \
-Dauto.commit.interval.ms=${DH_AUTO_COMMIT_INTERVAL_MS:-5000} \
-Dcom.devicehive.log.level=${DH_LOG_LEVEL:-WARN} \
-Denable.auto.commit=${DH_ENABLE_AUTO_COMMIT:-true} \
-Dfetch.max.wait.ms=${DH_FETCH_MAX_WAIT_MS:-100} \
-Dfetch.min.bytes=${DH_FETCH_MIN_BYTES:-1} \
-Dreplication.factor=${DH_REPLICATION_FACTOR:-1} \
-Droot.log.level=${ROOT_LOG_LEVEL:-WARN} \
-Dspring.datasource.url=jdbc:postgresql://${DH_POSTGRES_ADDRESS}:${DH_POSTGRES_PORT}/${DH_POSTGRES_DB} \
-Dspring.datasource.username="${DH_POSTGRES_USERNAME}" \
-Dspring.datasource.password="${DH_POSTGRES_PASSWORD}" \
-Dbootstrap.servers=${DH_KAFKA_ADDRESS}:${DH_KAFKA_PORT} \
-Dzookeeper.connect=${DH_ZK_ADDRESS}:${DH_ZK_PORT} \
-Dzookeeper.connectionTimeout=${DH_ZK_CONNECTIONTIMEOUT:-8000} \
-Dzookeeper.sessionTimeout=${DH_ZK_SESSIONTIMEOUT:-10000} \
-Dhazelcast.cluster.members=${HC_MEMBERS}:${HC_PORT} \
-Dhazelcast.group.name=${HC_GROUP_NAME} \
-Dhazelcast.group.password=${HC_GROUP_PASSWORD} \
-Drpc.server.request-consumer.threads=${DH_RPC_SERVER_REQ_CONS_THREADS:-3} \
-Drpc.server.worker.threads=${DH_RPC_SERVER_WORKER_THREADS:-3} \
./devicehive-backend-${DH_VERSION}-boot.jar &
PID=$!
wait $PID
