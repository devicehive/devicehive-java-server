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
  -o -z "$DH_RIAK_HOST" \
  -o -z "$DH_RIAK_PORT" ]
then
    echo "Some of required environment variables are not set or empty."
    echo "Please check following vars are passed to container:"
    echo "- DH_ZK_ADDRESS"
    echo "- DH_KAFKA_ADDRESS"
    echo "- DH_RIAK_HOST"
    echo "- DH_RIAK_PORT"
    exit 1
fi

echo "Starting DeviceHive frontend"
java -server -Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:+DisableExplicitGC -XX:+HeapDumpOnOutOfMemoryError -jar \
-Dcom.devicehive.log.level=${DH_LOG_LEVEL:-WARN} \
-Droot.log.level=${ROOT_LOG_LEVEL:-WARN} \
-Dflyway.enabled=false \
-Driak.host=${DH_RIAK_HOST} \
-Driak.port=${DH_RIAK_PORT} \
-Dbootstrap.servers=${DH_KAFKA_ADDRESS}:${DH_KAFKA_PORT:-9092} \
-Dzookeeper.connect=${DH_ZK_ADDRESS}:${DH_ZK_PORT:-2181} \
-Drpc.client.response-consumer.threads=${DH_RPC_CLIENT_RES_CONS_THREADS:-3} \
-Dserver.context-path=/api \
-Dserver.port=8080 \
./devicehive-frontend-${DH_VERSION}-boot.jar &
PID=$!
wait $PID
