#!/bin/bash -e

set -x

# Check if Zookeper, Kafka and Postgres are ready
while true; do
    `nc $DH_ZK_ADDRESS $DH_ZK_PORT`
    result_zk=$?
    `nc $DH_POSTGRES_ADDRESS $DH_POSTGRES_PORT`
    result_postgres=$?
    `nc $DH_KAFKA_ADDRESS $DH_KAFKA_PORT`
    result_kafka=$?

    if [ "$result_kafka" -eq 0 ] && [ "$result_postgres" -eq 0 ] && [ "$result_zk" -eq 0 ]; then
        break
    fi
    sleep 3
done

echo "Starting DeviceHive"
java -server -Xmx512m -XX:MaxRAMFraction=1 -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -XX:+ScavengeBeforeFullGC -XX:+CMSScavengeBeforeRemark -jar \
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

set +x