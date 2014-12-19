#!/bin/bash

SCRIPTNAME=`basename $0`
cd `dirname $0`
source defs.sh

# Check parameters
if [[ "$*" == "" ]]; then
    echo "Usage: $SCRIPTNAME PORT [DOMAIN LOGIN PASSWORD]"
    echo "Example:"
    echo "$SCRIPTNAME 8080 dh13.dataart.pen admin adminpassword"
    echo "  OR"
    echo "$SCRIPTNAME 8080"
    exit 1
fi

if [[ `id -u` != "0" ]]; then
    echo "Error: You must be root in order to build docker image"
    exit 2;
fi

# Setting variables
UUID=`uuidgen`

# Docker specific constants
DOCKER_POSTGRES="postgres:9.3.5" # Docker image for postgres database
DOCKER_DEVICEHIVE=${DH_DOCKER_NAME}:${DH_VERSION} # Docker image for DeviceHive Java server, defined in defs.sh

# Default constants
# PG_ - constants for Postgres
PG_CONTAINER="postgresql-$UUID" # Docker container name for postgres
PG_USER="dh_user" # username for database access
PG_DATABASE="dh" # database name
PG_DATADIR="/data/devicehive-$UUID" # directory which will be mapped on postgre docker container
PG_DATA="/data" # directory inside postgres container where database should be stored
PG_ACCESS_STRING="host all all 0.0.0.0/0 trust" # String which should be placed into pg_hba.conf
# DG_ - constants for DeviceHive
DH_CONTAINER="devicehive-$UUID" # Docker container name for DeviceHive Java server
DH_PROTOCOL="http" # Protocol for accessing DeviceHive instance from the internet
DH_DOMAIN="localhost" # Domain for accessing DeviceHive instance from the internet
DH_PORT="80" # Port for accessing DeviceHive instance from the internet


# Variables from parameters
DOCKER_PORT=$1 # Port where Docker daemon will bound the instance
if [[ "$2" != "" ]]; then
    DH_DOMAIN=$2 # override default value for the domain
fi
DH_LOGIN=$3 # Login for admin account in DeviceHive (user-defined)
DH_PASSWORD=$4 # Password for admin account in DeviceHive (user-defined)

echo "$UUID"

echo "Create devicehive-java server instance with PostgreSQL database"
echo "Step 1: Instantiate PostgreSQL database"
mkdir -p $PG_DATADIR
docker run -d -v $PG_DATADIR:$PG_DATA -e "PGDATA=$PG_DATA" --name $PG_CONTAINER $DOCKER_POSTGRES

echo "wait 10 seconds before going on..."
sleep 10

echo "Checking access to PostgreSQL service"
if ! grep -Fxq "$PG_ACCESS_STRING" $PG_DATADIR/pg_hba.conf; then
    docker stop $PG_CONTAINER > /dev/null
    echo -e "$PG_ACCESS_STRING" >> $PG_DATADIR/pg_hba.conf
    docker start $PG_CONTAINER > /dev/null
    echo "wait 10 seconds before going on..."
    sleep 10
fi

echo "Step 2: Create user and database in PostgreSQL container instance"
# new temp container laucnhed that inits the newly created postgresql server
echo "CREATE DATABASE $PG_DATABASE;CREATE USER $PG_USER;GRANT ALL privileges ON DATABASE $PG_DATABASE TO $PG_USER;\q" |
docker run -ti --link $PG_CONTAINER:postgres --rm $DOCKER_POSTGRES sh -c 'exec psql -h "$POSTGRES_PORT_5432_TCP_ADDR" -p "$POSTGRES_PORT_5432_TCP_PORT" -U postgres'

echo "Step 3: Migrate database"
docker run -ti --link $PG_CONTAINER:postgres --rm -e "PG_USER=$PG_USER" -e "PG_DATABASE=$PG_DATABASE" "${DOCKER_DEVICEHIVE}" sh -c 'java -jar /root/dh_dbtool.jar -migrate -url jdbc:postgresql://${POSTGRES_PORT_5432_TCP_ADDR}/${PG_DATABASE} -user ${PG_USER}'

echo "Step 4: Run GlassFish with DeviceHive"
docker run -d -p ${DOCKER_PORT}:8080 --name ${DH_CONTAINER} --link $PG_CONTAINER:postgres -e "PG_USER=$PG_USER" -e "PG_PASSWORD=$PG_PASSWORD" -e "PG_DATABASE=$PG_DATABASE" -e "DH_DOMAIN=$DH_DOMAIN" -e "DH_PORT=$DH_PORT" -e "DH_PROTOCOL=$DH_PROTOCOL" ${DOCKER_DEVICEHIVE}

if [[ "$DH_LOGIN" == "" || "$DH_PASSWORD" == "" ]]; then
    echo "No credentials specified, will not create user."
    echo "Step 5: skipping"
else
    echo "Step 5: Creating user $DH_LOGIN / $DH_PASSWORD"
    FINISH=0
    while [ $FINISH -eq 0 ]; do
        echo "Attempt to create user in 5 seconds:"
        sleep 5
        curl --fail "http://localhost:${DOCKER_PORT}/DeviceHive/rest/user" -H 'Pragma: no-cache' -H 'Accept-Encoding: gzip,deflate' -H 'Authorization: Basic ZGhhZG1pbjpkaGFkbWluXyM5MTE=' -H 'Content-Type: application/json' -H 'Accept: application/json, text/javascript, */*; q=0.01' -H 'Cache-Control: no-cache' -H 'X-Requested-With: XMLHttpRequest' -H 'Connection: keep-alive' --data-binary "{\"login\":\"${DH_LOGIN}\",\"status\":0,\"role\":0,\"networks\":[],\"password\":\"${DH_PASSWORD}\"}" --compressed
        if [ $? -eq 0 ]; then
            FINISH=1;
            echo "User created successfully"
        fi
    done
   
fi

echo "Step 6: Create nginx proxy server"
# Adjust nginx config parameters
sed -e "s/\${DH_PORT}/${DH_PORT}/g" -e "s/\${DH_DOMAIN}/${DH_DOMAIN}/g" -e "s/\${DOCKER_PORT}/${DOCKER_PORT}/g" nginx.conf \
> /etc/nginx/sites-enabled/devicehive-$UUID.conf

service nginx restart # restart nginx

echo "Step 7: Configure DeviceHive entry points"
FINISH=0
while [ $FINISH -eq 0 ]; do
    echo "Attempt to autoconfigure DeviceHive entrypoints"
    curl --fail "${DH_PROTOCOL}://${DH_DOMAIN}:${DH_PORT}/DeviceHive/rest/configuration/auto" -H 'Pragma: no-cache' -H 'Accept-Encoding: gzip,deflate' -H 'Authorization: Basic ZGhhZG1pbjpkaGFkbWluXyM5MTE=' -H 'Content-Type: application/x-www-form-urlencoded' -H 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8' -H 'Cache-Control: no-cache' -H 'Connection: keep-alive' -H "Referer: ${DH_PROTOCOL}://${DH_DOMAIN}:${DH_PORT}/DeviceHive/" --data 'input=Configure'
    if [ $? -eq 0 ]; then
        FINISH=1;
        echo "Autoconfigure finished"
        break
    fi
    echo "Retry in 5 seconds"
    sleep 5
done

echo ""
echo "SUCCESS"
echo ""
echo "${DH_PROTOCOL}://${DH_DOMAIN}:${DH_PORT}/admin/ - DeviceHive admin console"
echo "${DH_PROTOCOL}://${DH_DOMAIN}:${DH_PORT}/DeviceHive/ - Page with REST api and WebSocket entrypoints"
echo "UUID: $UUID"
