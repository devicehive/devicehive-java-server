#!/bin/bash -e

set -x

# Check if backend is ready
while true; do
    `nc $DH_BACKEND_ADDRESS $DH_BACKEND_HAZELCAST_PORT`
    result=$?

    if [ "$result" -eq 0 ]; then
        break
    fi
    sleep 3
done

echo "Starting DeviceHive"
java -server -Xmx512m -XX:MaxRAMFraction=1 -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -XX:+ScavengeBeforeFullGC -XX:+CMSScavengeBeforeRemark -jar \
-Dflyway.enabled=false \
-Driak.host=${DH_RIAK_HOST} \
-Driak.port=${DH_RIAK_PORT} \
-Dbootstrap.servers=${DH_KAFKA_ADDRESS}:${DH_KAFKA_PORT} \
-Dzookeeper.servers=${DH_ZK_ADDRESS}:${DH_ZK_PORT} \
-Drpc.client.response-consumer.threads=${DH_RPC_CLIENT_RES_CONS_THREADS:-1} \
-Dserver.context-path=/api \
-Dserver.port=8080 \
./devicehive-frontend-${DH_VERSION}-boot.jar

set +x
