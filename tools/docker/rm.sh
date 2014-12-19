#!/bin/bash

SCRIPTNAME=`basename $0`
if [[ "$*" == "" ]]; then
    echo "Usage: $SCRIPTNAME instance uuid"
    echo "Example: $SCRIPTNAME 8e36e72b-9501-47e6-abf7-d3d9581fdc8e"
    exit 1
fi

if [[ `id -u` != "0" ]]; then
    echo "Error: You must be root in order to remove DeviceHive instance"
    exit 2;
fi

UUID=$1
EXIT=0
env echo -n "Removing nginx virtual host..."
rm /etc/nginx/sites-enabled/devicehive-${UUID}.conf
if [ $? -eq 0 ]; then
    service nginx restart
    echo "SUCCESS"
else
    echo "FAIL:("
    EXIT=`expr $EXIT + 1`
fi

env echo -n "Removing DeviceHive docker container..."
docker rm -f devicehive-${UUID}
if [ $? -eq 0 ]; then
    echo "SUCCESS"
else
    echo "FAIL:("
    EXIT=`expr $EXIT + 2`
fi

env echo -n "Removing PostgreSQL docker container..."
docker rm -f postgresql-${UUID}
if [ $? -eq 0 ]; then
    echo "SUCCESS"
else
    echo "FAIL:("
    EXIT=`expr $EXIT + 4`
fi

env echo "Removing PostgreSQL database files..."
rm -R /data/devicehive-${UUID}
if [ $? -eq 0 ]; then
    echo "SUCCESS"
else
    echo "FAIL:("
    EXIT=`expr $EXIT + 8`
fi


exit $EXIT