#!/bin/bash -e

set -x

# Check if all required parameters are set
if [ -z "$DH_ZK_ADDRESS" \
  -o -z "$DH_KAFKA_ADDRESS" \
  -o -z "$DH_RIAK_HOST" \
  -o -z "$DH_RIAK_PORT" \
  -o -z "$DH_BACKEND_ADDRESS" ]
then
    echo "Some of required environment variables are not set or empty."
    echo "Please check following vars are passed to container:"
    echo "- DH_ZK_ADDRESS"
    echo "- DH_KAFKA_ADDRESS"
    echo "- DH_RIAK_HOST"
    echo "- DH_RIAK_PORT"
    echo "- DH_BACKEND_ADDRESS"
    exit 1
fi
# Check if backend is ready
until nc -v -z -w1 $DH_BACKEND_ADDRESS ${DH_BACKEND_HAZELCAST_PORT:-5701}
do
    sleep 3
done

echo "Starting DeviceHive frontend"
exec java -server -Xmx512m -XX:MaxRAMFraction=1 -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -XX:+ScavengeBeforeFullGC -XX:+CMSScavengeBeforeRemark -jar \
-Dflyway.enabled=false \
-Driak.host=${DH_RIAK_HOST} \
-Driak.port=${DH_RIAK_PORT} \
-Dbootstrap.servers=${DH_KAFKA_ADDRESS}:${DH_KAFKA_PORT:-9092} \
-Dzookeeper.connect=${DH_ZK_ADDRESS}:${DH_ZK_PORT:-2181} \
-Drpc.client.response-consumer.threads=${DH_RPC_CLIENT_RES_CONS_THREADS:-1} \
-Dserver.context-path=/api \
-Dserver.port=8080 \
./devicehive-frontend-${DH_VERSION}-boot.jar
