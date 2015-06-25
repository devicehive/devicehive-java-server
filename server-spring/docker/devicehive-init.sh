#!/bin/bash

if [ ! -f "/init_completed" ]; then
	if [ ! -z "$REDIS_MEM" ]; then
		sed -i -e "s/^\# maxmemory <bytes>/maxmemory ${REDIS_MEM}mb/" /etc/redis/redis.conf
	else
		sed -i -e "s/^\# maxmemory <bytes>/maxmemory 1024mb/" /etc/redis/redis.conf
	fi
	pg_ctlcluster 9.3 main start
	echo "CREATE DATABASE dh;CREATE USER dhadmin WITH password 'dhadmin_#911';GRANT ALL privileges ON DATABASE dh TO dhadmin;\q" | psql -U postgres
	java -jar /root/dh_dbtool.jar -migrate -url jdbc:postgresql://127.0.0.1:5432/dh -user dhadmin -password 'dhadmin_#911'
	echo "update configuration set value=1 where name='threads.count';\q" | psql -U postgres dh
	pg_ctlcluster 9.3 main stop
	touch /init_completed
fi

/usr/bin/supervisord -c /etc/supervisor/supervisord.conf

trap "{ supervisorctl stop devicehive; \
	supervisorctl stop kafka; \
	supervisorctl stop postgresql; \
	supervisorctl stop zookeeper;
	supervisorctl stop sshd; \
	killall supervisord; \
	exit 0; }" SIGINT SIGTERM SIGKILL
while :
do
        sleep 1
done
