#!/bin/bash -e

set -x

# Check if all required parameters are set
if [ -z "$DH_ZK_ADDRESS" \
  -o -z "$DH_KAFKA_ADDRESS" \
  -o -z "$DH_POSTGRES_ADDRESS" \
  -o -z "$DH_POSTGRES_USERNAME" \
  -o -z "$DH_POSTGRES_PASSWORD" \
  -o -z "$DH_POSTGRES_DB" ]
then
    echo "Some of required environment variables are not set or empty."
    echo "Please check following vars are passed to container:"
    echo "- DH_ZK_ADDRESS"
    echo "- DH_KAFKA_ADDRESS"
    echo "- DH_POSTGRES_ADDRESS"
    echo "- DH_POSTGRES_USERNAME"
    echo "- DH_POSTGRES_PASSWORD"
    echo "- DH_POSTGRES_DB"
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

    if [ "$result_kafka" -eq 0 -a "$result_postgres" -eq 0 -a "$result_zk" -eq 0 ]; then
        break
    fi
    sleep 3
done

echo "Starting DeviceHive backend"
exec java -server -Xmx512m -XX:MaxRAMFraction=1 -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -XX:+ScavengeBeforeFullGC -XX:+CMSScavengeBeforeRemark -jar \
-Dspring.datasource.url=jdbc:postgresql://${DH_POSTGRES_ADDRESS}:${DH_POSTGRES_PORT}/${DH_POSTGRES_DB} \
-Dspring.datasource.username="${DH_POSTGRES_USERNAME}" \
-Dspring.datasource.password="${DH_POSTGRES_PASSWORD}" \
-Dbootstrap.servers=${DH_KAFKA_ADDRESS}:${DH_KAFKA_PORT} \
-Dzookeeper.connect=${DH_ZK_ADDRESS}:${DH_ZK_PORT} \
-Dhazelcast.port=${DH_HAZELCAST_PORT:-5701} \
-Drpc.server.request-consumer.threads=${DH_RPC_SERVER_REQ_CONS_THREADS:-1} \
-Drpc.server.worker.threads=${DH_RPC_SERVER_WORKER_THREADS:-1} \
-Drpc.server.disruptor.wait-strategy=${DH_RPC_SERVER_DISR_WAIT_STRATEGY:-blocking} \
./devicehive-backend-${DH_VERSION}-boot.jar
