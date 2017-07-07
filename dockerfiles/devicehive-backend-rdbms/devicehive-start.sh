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
java -server -Xmx512m -XX:MaxRAMFraction=1 -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -XX:+ScavengeBeforeFullGC -XX:+CMSScavengeBeforeRemark -jar \
-Dcom.devicehive.log.level=${DH_LOG_LEVEL:-INFO} \
-Droot.log.level=${ROOT_LOG_LEVEL:-INFO} \
-Dspring.datasource.url=jdbc:postgresql://${DH_POSTGRES_ADDRESS}:${DH_POSTGRES_PORT}/${DH_POSTGRES_DB} \
-Dspring.datasource.username="${DH_POSTGRES_USERNAME}" \
-Dspring.datasource.password="${DH_POSTGRES_PASSWORD}" \
-Dbootstrap.servers=${DH_KAFKA_ADDRESS}:${DH_KAFKA_PORT} \
-Dzookeeper.connect=${DH_ZK_ADDRESS}:${DH_ZK_PORT} \
-Dhazelcast.cluster.members=${HC_MEMBERS}:${HC_PORT} \
-Dhazelcast.group.name=${HC_GROUP_NAME} \
-Dhazelcast.group.password=${HC_GROUP_PASSWORD} \
-Drpc.server.request-consumer.threads=${DH_RPC_SERVER_REQ_CONS_THREADS:-1} \
-Drpc.server.worker.threads=${DH_RPC_SERVER_WORKER_THREADS:-1} \
-Drpc.server.disruptor.wait-strategy=${DH_RPC_SERVER_DISR_WAIT_STRATEGY:-blocking} \
./devicehive-backend-${DH_VERSION}-boot.jar &
PID=$!
wait $PID
